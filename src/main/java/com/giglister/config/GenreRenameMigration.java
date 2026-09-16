package com.giglister.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * "Progressive" (see GenreTaxonomy) was split into "Progressive Rock" and "Progressive
 * Metal" - GenreTaxonomy.BASE_GENRES no longer offers the old name, so anyone who'd already
 * picked it as a preferred genre would otherwise be stuck with an orphaned value the profile
 * picker can't even show a button for anymore (UserService only ever validates new writes
 * against the current BASE_GENRES, it doesn't touch what's already stored). Same situation
 * EnumCheckConstraintSync exists for - no Flyway/Liquibase here to carry a one-off data
 * migration, so it runs as a plain UPDATE on every startup instead; it naturally does
 * nothing once every "Progressive" row is gone; picks Rock, the more common of the two, as
 * the closer default for a preference that was never more specific than "Progressive".
 */
@Component
@Slf4j
public class GenreRenameMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public GenreRenameMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE user_preferred_genre SET genre = 'Progressive Rock' WHERE genre = 'Progressive'");
            if (updated > 0) {
                log.info("Migrated {} preferred-genre row(s) from 'Progressive' to 'Progressive Rock'", updated);
            }
        } catch (Exception e) {
            log.warn("Could not migrate 'Progressive' preferred-genre rows - leaving them as-is: {}", e.getMessage());
        }
    }
}
