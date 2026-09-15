package com.giglister.config;

import com.giglister.domain.enums.BandImageDisplay;
import com.giglister.domain.enums.ClaimStatus;
import com.giglister.domain.enums.EntityStatus;
import com.giglister.domain.enums.EntityType;
import com.giglister.domain.enums.EventStatus;
import com.giglister.domain.enums.PermissionLevel;
import com.giglister.domain.enums.SubmissionStatus;
import com.giglister.domain.enums.SubmissionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hibernate's ddl-auto=update creates a CHECK(col IN (...)) constraint for every
 * {@code @Enumerated(EnumType.STRING)} column, baked in with whatever enum values
 * exist the first time that table is created - but it never widens that constraint
 * when the Java enum later gains a new constant (ddl-auto=update only ever adds
 * tables/columns, it never alters an existing constraint). On a long-lived
 * deployment with no Flyway/Liquibase here, that's a live landmine: EntityType
 * gained EVENT_SERIES for the Reihen feature, and every attempt to grant a
 * permission on a new EventSeries then failed with "new row ... violates check
 * constraint entity_permission_entity_type_check" - a 500 on the simple act of
 * creating a Reihe, months after the column itself had existed just fine.
 *
 * Runs once after Hibernate's own schema update on every startup and re-creates
 * each of these constraints from the CURRENT enum values, so this can't recur for
 * any of them again - self-healing on the next deploy instead of needing a one-off
 * manual ALTER TABLE against the live database.
 */
@Component
@Slf4j
public class EnumCheckConstraintSync implements ApplicationRunner {

    private record Spec(String table, String column, List<String> values) {
        String constraintName() {
            return table + "_" + column + "_check";
        }
    }

    private final JdbcTemplate jdbcTemplate;

    public EnumCheckConstraintSync(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Spec> specs = List.of(
                new Spec("entity_permission", "entity_type", values(EntityType.class)),
                new Spec("entity_permission", "permission", values(PermissionLevel.class)),
                new Spec("claim", "entity_type", values(EntityType.class)),
                new Spec("claim", "status", values(ClaimStatus.class)),
                new Spec("entity_merge", "entity_type", values(EntityType.class)),
                new Spec("band", "status", values(EntityStatus.class)),
                new Spec("location", "status", values(EntityStatus.class)),
                new Spec("event", "status", values(EventStatus.class)),
                new Spec("event", "band_image_display", values(BandImageDisplay.class)),
                new Spec("submission", "type", values(SubmissionType.class)),
                new Spec("submission", "status", values(SubmissionStatus.class))
        );
        for (Spec spec : specs) {
            resync(spec);
        }
    }

    private void resync(Spec spec) {
        String valuesList = spec.values().stream().map(v -> "'" + v + "'").collect(Collectors.joining(", "));
        try {
            jdbcTemplate.execute("ALTER TABLE " + spec.table() + " DROP CONSTRAINT IF EXISTS " + spec.constraintName());
            jdbcTemplate.execute("ALTER TABLE " + spec.table() + " ADD CONSTRAINT " + spec.constraintName()
                    + " CHECK (" + spec.column() + " IN (" + valuesList + "))");
        } catch (Exception e) {
            // Never block startup over this - a stale-but-present constraint is exactly the
            // bug this exists to fix, not a reason to also take the whole app down.
            log.warn("Could not sync enum check constraint {} - leaving it as-is: {}", spec.constraintName(), e.getMessage());
        }
    }

    private <E extends Enum<E>> List<String> values(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).toList();
    }
}
