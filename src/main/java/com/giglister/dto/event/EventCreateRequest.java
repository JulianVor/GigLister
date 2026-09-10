package com.giglister.dto.event;

import com.giglister.dto.common.EntityRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventCreateRequest(
        String title,
        @NotNull LocalDate date,
        LocalTime startTime,
        @NotNull @Valid EntityRef location,
        @NotEmpty @Valid List<EntityRef> bands,
        String description,
        String ticketUrl,
        String titleImageUrl
) {
}
