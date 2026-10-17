package com.chuseok22.whoim.server.interview.core.constant;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;

// UNKNOWN이면 content, source는 null
public record TraceStateEntry(
    TraceElementStatus status,
    String content,
    EvidenceLinkType source,
    int turnNumber
) {

}
