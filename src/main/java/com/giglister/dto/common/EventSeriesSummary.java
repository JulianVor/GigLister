package com.giglister.dto.common;

/** Embedded in EventSummary/EventResponse so a card/detail page can show "Teil von
 * <name>" and link straight to the series without a second request. Carries the series'
 * own ticketUrl too - a festival's shared ticket link, where set, is the one sensible page
 * for every concert in it, overriding that event's own (see EventDetailPage's ticketUrl). */
public record EventSeriesSummary(
        Long id,
        String name,
        String titleImageUrl,
        String ticketUrl
) {
}
