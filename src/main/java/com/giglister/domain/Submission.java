package com.giglister.domain;

import com.giglister.domain.enums.SubmissionStatus;
import com.giglister.domain.enums.SubmissionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A proposal from an external source (the GPT-skill integration) to create a
 * Band/Location/Event, or (see targetEntityId) to enrich an existing STUB/DRAFT
 * Band/Location - never changes anything by itself. A platform admin reviews it
 * and either approves (which runs the normal create/patch flow) or rejects it;
 * nothing changes in the live data until approved.
 */
@Entity
@Table(name = "submission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionType type;

    /** Raw JSON matching BandCreateRequest/LocationCreateRequest/EventCreateRequest, depending
     * on type. Plain TEXT, deliberately not @Lob - Hibernate maps @Lob String to Postgres's
     * OID-based Large Object type, which requires every read (not just writes) to happen
     * inside an explicit transaction; AdminService.dashboard() and friends aren't
     * @Transactional (they don't need to be, for everything else they read), so loading a
     * Submission there failed with "Large Objects may not be used in auto-commit mode" the
     * moment a real row existed to read back - never caught locally since nothing had
     * exercised that path against a populated table before. TEXT has no such restriction and
     * comfortably holds this (a few hundred bytes of JSON at most). */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    /** External URL to fetch and store only once approved - never downloaded before then. */
    private String imageUrl;

    /** Null: propose a brand-new Band/Location/Event (the original, only behavior). Set (BAND
     * or LOCATION only): this is instead an enrichment for an existing STUB/DRAFT entity -
     * on approval its non-null payload fields are patched onto that entity (never replacing
     * ones the payload left out), and it's bumped to PUBLISHED once the result looks complete
     * (see BandService/LocationService#isComplete). */
    private Long targetEntityId;

    /** Null for the GPT-skill integration (it has no giglister account of its own, so the
     * approving admin is attributed as the result entity's creator - see SubmissionService).
     * Set when a logged-in user without direct create rights submitted this themselves (see
     * EventController.create) - on approval THEY become the result entity's creator, not the
     * approving admin, so they keep normal edit/delete rights over what they proposed. */
    private Long submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(nullable = false)
    private Instant submittedAt;

    private Long reviewedBy;

    private Instant reviewedAt;

    @Column(length = 1000)
    private String rejectionReason;

    /** The Band/Location/Event id created on approval. */
    private Long resultEntityId;

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
    }
}
