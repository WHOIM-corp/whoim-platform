package com.chuseok22.whoim.server.consultation.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.consultation.application.dto.request.ConsultationQuestionsRequest;
import com.chuseok22.whoim.server.consultation.application.dto.request.ConsultationSuggestionsRequest;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationQuestionsResponse;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationSuggestionsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Consultation", description = "후임자 상담 API (저장하지 않는 2단계: 추가 질문 -> Card 제시)")
public interface ConsultationControllerDocs {

  @Operation(
      summary = "상담 추가 질문 받기",
      description = """
          후임자가 쓴 상황을 보내면, 본인에게 지정된 PUBLISHED Package의 APPROVED Card만 후보로 삼아 AI가 부족한 맥락을 묻는 질문을 0~3개 돌려준다.
          후보 Card가 없으면 AI를 호출하지 않고 questions를 비운 채 안내 문구(message)를 돌려준다.
          후보는 프롬프트 크기 제한으로 최근 수정된 Card 6개까지만 사용한다.

          실패 케이스
          - 400 INVALID_REQUEST: 상황이 비어 있거나 2000자를 넘음
          - 403 ACCESS_DENIED: 후임자가 아님
          - 503 LLM_UNAVAILABLE: AI 응답을 가져올 수 없거나 형식이 올바르지 않음(재시도 없음)
          """)
  ResponseEntity<ConsultationQuestionsResponse> questions(
      AuthenticatedMember requester, ConsultationQuestionsRequest request);

  @Operation(
      summary = "상담 Card 제시받기",
      description = """
          상황과 추가 질문에 대한 답변(없으면 빈 목록)을 보내면, 후보 Card 중 관련 있는 Card를 최대 2개 제시한다.
          각 Card에는 원 필드, 근거(종류, 업무 기록 파일명, 발췌), 검증 시각, AI가 쓴 설명(explanation)이 담긴다.
          후보가 없으면 AI를 호출하지 않고, 관련 Card가 없으면 cards를 비운 채 안내 문구(message)를 돌려준다.

          실패 케이스
          - 400 INVALID_REQUEST: 상황이 비어 있거나 2000자를 넘음, 답변이 4개 이상이거나 비어 있음
          - 403 ACCESS_DENIED: 후임자가 아님
          - 503 LLM_UNAVAILABLE: AI 응답을 가져올 수 없거나 형식이 올바르지 않음(재시도 없음)
          """)
  ResponseEntity<ConsultationSuggestionsResponse> suggestions(
      AuthenticatedMember requester, ConsultationSuggestionsRequest request);
}
