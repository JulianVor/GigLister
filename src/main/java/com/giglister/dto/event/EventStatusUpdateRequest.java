package com.giglister.dto.event;

import com.giglister.domain.enums.EventStatus;
import jakarta.validation.constraints.NotNull;

public record EventStatusUpdateRequest(@NotNull EventStatus status) {
}
