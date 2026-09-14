package com.giglister.dto.common;

/** Embedded in EventSummary/EventResponse so a card/detail page can show "Teil von
 * <name>" and link straight to the series without a second request. */
public record EventSeriesSummary(
        Long id,
        String name,
        String titleImageUrl
) {
}
