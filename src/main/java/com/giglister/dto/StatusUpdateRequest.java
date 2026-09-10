package com.giglister.dto;

import com.giglister.domain.enums.EntityStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull EntityStatus status) {
}
