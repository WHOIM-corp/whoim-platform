package com.chuseok22.whoim.server.card.application.dto.data;

// 항목 하나가 잘못돼도 응답 전체를 버리지 않도록 검증 어노테이션을 두지 않고, CardService가 항목별로 걸러낸다
public record DirectEvidenceCandidate(Integer turnNumber, String excerpt) {

}
