package com.chuseok22.whoim.server.dlp.application.dto.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DlpAiDetectionResult(
    @NotNull List<@Valid DlpDetection> detections
) {
}
