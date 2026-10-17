package com.chuseok22.whoim.server.dlp.application.dto.data;

import jakarta.validation.constraints.NotBlank;

public record DlpDetection(
    @NotBlank String text,
    String type
) {
}
