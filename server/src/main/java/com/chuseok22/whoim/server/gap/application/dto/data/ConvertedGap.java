package com.chuseok22.whoim.server.gap.application.dto.data;

import com.chuseok22.whoim.server.gap.core.constant.GapType;
import java.util.List;

public record ConvertedGap(
    GapType type,
    String summary,
    String question,
    List<ResolvedAnchor> anchors
) {
}
