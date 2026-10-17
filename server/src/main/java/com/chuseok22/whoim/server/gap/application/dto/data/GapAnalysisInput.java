package com.chuseok22.whoim.server.gap.application.dto.data;

import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import java.util.List;

// documents의 인덱스 i가 프롬프트의 라벨 D{i+1}이다
public record GapAnalysisInput(
    String text,
    List<EvidenceDocument> documents
) {
}
