package com.giglister.dto.band;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BandStoryCreateRequest(
        @NotBlank String imageUrl,
        @Size(max = 280) String text,
        // How the band positioned/scaled/rotated imageUrl within the 9:16 story frame - see
        // BandStory.imgWidthPct and BandStoryComposer on the frontend. All five together or
        // none - the frontend always sends all five.
        Double imgWidthPct,
        Double imgHeightPct,
        Double imgCenterXPct,
        Double imgCenterYPct,
        Double imgRotationDeg,
        String imgBackgroundColor,
        // Opaque to the backend - see BandStory.textLayersJson.
        String textLayersJson,
        // Opaque to the backend - see BandStory.bandTagsJson.
        String bandTagsJson
) {
}
