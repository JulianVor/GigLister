package com.giglister.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * One band's slot within an Event's line-up: the band's identity plus, optionally, its
 * own start time within the show - distinct from the Event's own overall startTime, for
 * a festival day where each band goes on stage at a different time. Null startTime means
 * this band shares the event's own overall time (the common case).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BandLineupEntry {

    @Column(name = "band_id", nullable = false)
    private Long bandId;

    @Column(name = "start_time")
    private LocalTime startTime;
}
