package com.chuseok22.whoim.server.card.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.application.dto.response.CardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "Card", description = "TRACE Card API")
public interface CardControllerDocs {

  @Operation(
      summary = "Card 생성",
      description = """
          종료된 인터뷰의 TRACE 상태로 TRACE Card(DRAFT)를 만든다. 필드는 채워진 요소만 쓰고, 근거는 Gap 앵커(RECORDED)와
          서버가 답변 원문과 대조해 통과시킨 전임자 답변 발췌(DIRECT)다. 인터뷰당 Card는 하나다.

          실패 케이스
          - 403 ACCESS_DENIED: 전임자가 아니거나 남의 인터뷰
          - 404 RESOURCE_NOT_FOUND: 인터뷰가 없음
          - 409 STATE_CONFLICT: PUBLISHED, 인터뷰 미종료, 이미 Card가 있음, 전임자 답변으로 채워진 요소가 없음, 동시 처리
          - 503 LLM_UNAVAILABLE: AI 응답을 가져오지 못했거나 유효한 직접 근거가 없음(저장 없음)
          """)
  ResponseEntity<CardResponse> create(AuthenticatedMember requester, UUID interviewId);
}
