package com.chuseok22.whoim.server.card.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.application.dto.request.CardRejectRequest;
import com.chuseok22.whoim.server.card.application.dto.response.CardDetailResponse;
import com.chuseok22.whoim.server.card.application.dto.response.CardSummaryResponse;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "Card Review", description = "TRACE Card 조회·Confirm·Approve API")
public interface CardReviewControllerDocs {

  @Operation(
      summary = "Card 목록 조회",
      description = """
          전임자는 본인 Package의 Card, 승인자는 DRAFT를 제외한 전체 Card를 최근 수정 순으로 반환한다.
          status를 주면 해당 상태만 거른다(승인자가 DRAFT를 주면 빈 목록).

          실패 케이스
          - 400 INVALID_REQUEST: status 값이 올바르지 않음
          - 403 ACCESS_DENIED: 전임자·승인자가 아님
          """)
  ResponseEntity<List<CardSummaryResponse>> list(AuthenticatedMember requester, CardStatus status);

  @Operation(
      summary = "Card 상세 조회",
      description = """
          Card 필드, 근거, 상태 이력을 반환한다.

          실패 케이스
          - 403 ACCESS_DENIED: 남의 Package의 Card(전임자)
          - 404 RESOURCE_NOT_FOUND: Card가 없음, 승인자가 DRAFT Card를 조회
          """)
  ResponseEntity<CardDetailResponse> get(AuthenticatedMember requester, UUID cardId);

  @Operation(
      summary = "전임자 Confirm",
      description = """
          DRAFT Card를 PREDECESSOR_CONFIRMED로 바꾸고 상태 이력을 남긴다.

          실패 케이스
          - 403 ACCESS_DENIED: 전임자가 아니거나 남의 Card
          - 404 RESOURCE_NOT_FOUND: Card가 없음
          - 409 STATE_CONFLICT: Package가 PUBLISHED, Card가 DRAFT가 아님
          """)
  ResponseEntity<CardDetailResponse> confirm(AuthenticatedMember requester, UUID cardId);

  @Operation(
      summary = "승인자 Approve",
      description = """
          PREDECESSOR_CONFIRMED Card를 APPROVED로 바꾸고 상태 이력을 남긴다.

          실패 케이스
          - 403 ACCESS_DENIED: 승인자가 아님
          - 404 RESOURCE_NOT_FOUND: Card가 없음
          - 409 STATE_CONFLICT: Package가 PUBLISHED, 근거가 없음, Card가 PREDECESSOR_CONFIRMED가 아님(DRAFT 포함)
          """)
  ResponseEntity<CardDetailResponse> approve(AuthenticatedMember requester, UUID cardId);

  @Operation(
      summary = "승인자 Reject",
      description = """
          PREDECESSOR_CONFIRMED Card를 REJECTED로 바꾸고 사유와 함께 상태 이력을 남긴다. Card는 삭제하지 않는다.

          실패 케이스
          - 400 INVALID_REQUEST: 거절 사유가 비어 있거나 500자를 넘음
          - 400 MALFORMED_REQUEST_BODY: 요청 본문이 없거나 형식이 올바르지 않음
          - 403 ACCESS_DENIED: 승인자가 아님
          - 404 RESOURCE_NOT_FOUND: Card가 없음
          - 409 STATE_CONFLICT: Package가 PUBLISHED, Card가 PREDECESSOR_CONFIRMED가 아님(DRAFT 포함)
          """)
  ResponseEntity<CardDetailResponse> reject(AuthenticatedMember requester, UUID cardId, CardRejectRequest request);
}
