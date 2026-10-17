package com.chuseok22.whoim.server.interview.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.interview.application.dto.request.InterviewAnswerRequest;
import com.chuseok22.whoim.server.interview.application.dto.request.InterviewStartRequest;
import com.chuseok22.whoim.server.interview.application.dto.response.InterviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "Interview", description = "전임자 AI 인터뷰 API")
public interface InterviewControllerDocs {

  @Operation(
      summary = "인터뷰 시작",
      description = """
          전임자가 자신의 Gap 하나로 인터뷰를 시작한다. AI가 첫 질문을 만들어 1번째 턴으로 저장한다. Gap당 인터뷰는 하나다.

          실패 케이스
          - 400 INVALID_REQUEST: gapId 누락
          - 403 ACCESS_DENIED: 전임자가 아니거나 남의 Gap
          - 404 RESOURCE_NOT_FOUND: Gap이 없음
          - 409 STATE_CONFLICT: PUBLISHED이거나 이미 인터뷰가 있음
          - 503 LLM_UNAVAILABLE: AI 응답을 가져오지 못함(저장 없음)
          """)
  ResponseEntity<InterviewResponse> start(AuthenticatedMember requester, InterviewStartRequest request);

  @Operation(
      summary = "인터뷰 답변 제출",
      description = """
          마지막 질문에 답한다. 답변은 먼저 저장되고, AI가 TRACE 요소를 추출한 뒤 서버 규칙으로 종료 여부를 정한다.
          종료는 10개 요소가 모두 채워졌거나(CHECKLIST_COMPLETE) 5번째 답변을 처리했을 때(TURN_LIMIT)다.
          AI 호출이 실패하거나 다음 질문이 비어 있으면 503이지만 답변은 보존되며 retry로 이어간다.

          실패 케이스
          - 400 INVALID_REQUEST: 답변이 비었거나 4000자 초과
          - 403 ACCESS_DENIED: 전임자가 아니거나 남의 인터뷰
          - 404 RESOURCE_NOT_FOUND: 인터뷰가 없음
          - 409 STATE_CONFLICT: PUBLISHED, 종료된 인터뷰, 마지막 질문이 이미 답변됨, 동시 처리
          - 503 LLM_UNAVAILABLE: AI 응답을 가져오지 못함(답변은 보존)
          """)
  ResponseEntity<InterviewResponse> submitAnswer(
      AuthenticatedMember requester, UUID interviewId, InterviewAnswerRequest request);

  @Operation(
      summary = "인터뷰 재처리",
      description = """
          답변은 저장됐지만 AI 추출이 끝나지 않은 마지막 턴을 같은 답변으로 다시 처리한다.

          실패 케이스
          - 403 ACCESS_DENIED: 전임자가 아니거나 남의 인터뷰
          - 404 RESOURCE_NOT_FOUND: 인터뷰가 없음
          - 409 STATE_CONFLICT: 처리할 턴이 없음(미답변, 이미 처리됨, 종료됨), PUBLISHED, 동시 처리
          - 503 LLM_UNAVAILABLE: AI 응답을 가져오지 못함
          """)
  ResponseEntity<InterviewResponse> retry(AuthenticatedMember requester, UUID interviewId);

  @Operation(
      summary = "인터뷰 조회",
      description = """
          인터뷰의 턴, 현재 질문, TRACE 채움 상태를 반환한다. 전임자는 본인 것만, 승인자는 전체를 본다.

          실패 케이스
          - 403 ACCESS_DENIED: 권한 없음 또는 남의 인터뷰
          - 404 RESOURCE_NOT_FOUND: 인터뷰가 없음
          """)
  ResponseEntity<InterviewResponse> get(AuthenticatedMember requester, UUID interviewId);
}
