package com.giglister.dto.band;

/** One typeahead result for tagging another band onto a story (see BandStoryComposer's
 * "+ Band") - just enough to render a square picture and a name, nothing a full BandResponse
 * carries (upcomingEvents etc.) is needed here. */
public record BandTagOptionResponse(
        Long id,
        String name,
        String city,
        String profileImageUrl,
        String logoUrl
) {
}
