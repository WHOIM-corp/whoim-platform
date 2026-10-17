package com.chuseok22.whoim.server.gap.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.gap.application.dto.response.GapResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "Gap", description = "Gap 분석 API")
public interface GapControllerDocs {

  @Operation(
      summary = "Gap 분석 시작",
      description = """
          전임자가 자신의 Package 문서 전체를 AI로 분석해 Gap(판단의 빈틈)을 만든다. Package당 1회만 가능하다.
          유효한 Gap이 없으면 저장 없이 빈 목록을 반환하며 다시 실행할 수 있다. 응답의 진행 상태는 항상 NOT_STARTED다.

          실패 케이스
          - 400 GAP_ANALYSIS_NO_DOCUMENTS: Package에 문서가 없음
          - 400 GAP_ANALYSIS_INPUT_TOO_LARGE: 문서 분량이 분석 입력 상한을 넘음
          - 403 ACCESS_DENIED: 전임자가 아니거나 남의 Package
          - 404 RESOURCE_NOT_FOUND: Package가 없음
          - 409 STATE_CONFLICT: PUBLISHED이거나 이미 분석됨, 또는 분석 중 문서/분석이 변경됨
          - 503 LLM_UNAVAILABLE: AI 응답을 가져오지 못함(저장 없음)
          """)
  ResponseEntity<List<GapResponse>> analyze(AuthenticatedMember requester, UUID handoverPackageId);

  @Operation(
      summary = "Gap 목록 조회",
      description = """
          Package의 Gap, 근거 앵커, 진행 상태(NOT_STARTED, IN_PROGRESS, COMPLETED, PARTIAL, REJECTED)를 반환한다.
          전임자는 자신의 Package만, 승인자는 전체를 본다.

          실패 케이스
          - 400 INVALID_REQUEST: handoverPackageId 누락 또는 UUID 형식 오류
          - 403 ACCESS_DENIED: 권한 없음 또는 남의 Package
          - 404 RESOURCE_NOT_FOUND: Package가 없음
          """)
  ResponseEntity<List<GapResponse>> list(AuthenticatedMember requester, UUID handoverPackageId);
}
