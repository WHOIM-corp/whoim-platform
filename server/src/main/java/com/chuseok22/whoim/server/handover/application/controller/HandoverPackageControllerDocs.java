package com.chuseok22.whoim.server.handover.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageCreateRequest;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageSuccessorRequest;
import com.chuseok22.whoim.server.handover.application.dto.response.HandoverPackageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "Handover Package", description = "Handover Package API")
public interface HandoverPackageControllerDocs {

  @Operation(
      summary = "Package 생성",
      description = """
          승인자가 전임자(필수)와 후임자(선택)를 지정해 WRITING 상태의 Package를 만든다.

          실패 케이스
          - 400 INVALID_REQUEST: 필수값 누락, 길이 초과, 전임자/후임자의 position이 맞지 않음
          - 403 ACCESS_DENIED: 승인자가 아님
          - 404 RESOURCE_NOT_FOUND: 지정한 회원이 없음
          - 409 STATE_CONFLICT: 같은 전임자에게 같은 이름의 Package가 이미 있음
          """)
  ResponseEntity<HandoverPackageResponse> create(HandoverPackageCreateRequest request);

  @Operation(
      summary = "후임자 지정/해제",
      description = """
          승인자가 Package의 후임자를 지정한다. successorId가 null이면 해제한다.

          실패 케이스
          - 400 INVALID_REQUEST: 후임자의 position이 SUCCESSOR가 아님
          - 403 ACCESS_DENIED: 승인자가 아님
          - 404 RESOURCE_NOT_FOUND: Package 또는 후임자가 없음
          - 409 STATE_CONFLICT: PUBLISHED Package
          """)
  ResponseEntity<HandoverPackageResponse> updateSuccessor(UUID handoverPackageId, HandoverPackageSuccessorRequest request);

  @Operation(
      summary = "Package 목록 조회",
      description = """
          최신순 목록을 반환한다. 승인자는 전체, 전임자는 본인 Package만 본다.
          후임자는 본인에게 지정된 PUBLISHED Package만 보며, cards와 progress는 APPROVED Card만으로 계산한다.
          후임자에게도 Package의 predecessor.username(로그인 ID)이 그대로 나간다. 수용한 노출이다.

          실패 케이스
          - 403 ACCESS_DENIED: 전임자·승인자·후임자가 아님
          """)
  ResponseEntity<List<HandoverPackageResponse>> list(AuthenticatedMember requester);

  @Operation(
      summary = "Package 상세 조회",
      description = """
          승인자는 전체, 전임자는 본인 Package만 조회한다.
          후임자는 본인에게 지정된 PUBLISHED Package만 조회하며, cards와 progress는 APPROVED Card만으로 계산한다.

          실패 케이스
          - 400 INVALID_REQUEST: 경로의 ID가 UUID 형식이 아님
          - 403 ACCESS_DENIED: 권한 없음 또는 남의 Package, 후임자가 지정되지 않았거나 아직 PUBLISHED가 아닌 Package(후임자)
          - 404 RESOURCE_NOT_FOUND: Package가 없음
          """)
  ResponseEntity<HandoverPackageResponse> get(AuthenticatedMember requester, UUID handoverPackageId);

  @Operation(
      summary = "Package 공개",
      description = """
          승인자가 후임자가 지정되고 활성 Card(REJECTED 제외)가 모두 APPROVED인 Package를 PUBLISHED로 바꾼다.

          실패 케이스
          - 400 INVALID_REQUEST: 경로의 ID가 UUID 형식이 아님
          - 403 ACCESS_DENIED: 승인자가 아님
          - 404 RESOURCE_NOT_FOUND: Package가 없음
          - 409 STATE_CONFLICT: 이미 PUBLISHED, 후임자 없음, 활성 Card가 없거나 미승인 Card가 있음
          """)
  ResponseEntity<HandoverPackageResponse> publish(AuthenticatedMember requester, UUID handoverPackageId);
}
