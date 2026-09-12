package com.giglister.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenRequest(@NotBlank String token) {
}
