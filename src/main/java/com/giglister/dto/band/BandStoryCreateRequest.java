package com.giglister.dto.band;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BandStoryCreateRequest(
        @NotBlank String imageUrl,
        @Size(max = 280) String text,
        // How the band positioned/zoomed imageUrl within the 9:16 story frame - see
        // BandStory.imgWidthPct and BandStoryComposer on the frontend. All four together or
        // none - the frontend always sends all four.
        Double imgWidthPct,
        Double imgHeightPct,
        Double imgOffsetLeftPct,
        Double imgOffsetTopPct
) {
}
