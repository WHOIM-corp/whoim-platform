package com.chuseok22.whoim.server.gap.application.dto.data;

import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;

public record ResolvedAnchor(
    EvidenceDocument document,
    int startOffset,
    int endOffset,
    String excerpt
) {
}
