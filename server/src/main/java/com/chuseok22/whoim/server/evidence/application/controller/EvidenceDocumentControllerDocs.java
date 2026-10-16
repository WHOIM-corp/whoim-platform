package com.chuseok22.whoim.server.evidence.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.evidence.application.dto.response.EvidenceDocumentDetailResponse;
import com.chuseok22.whoim.server.evidence.application.dto.response.EvidenceDocumentResponse;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Evidence", description = "Evidence 문서 API")
public interface EvidenceDocumentControllerDocs {

  @Operation(
      summary = "Evidence 문서 업로드",
      description = """
          전임자가 자신의 Package에 .md 또는 .txt 문서를 올린다. 분석 시작 전(Gap이 없고 PUBLISHED가 아닌 Package)에만 가능하다.

          실패 케이스
          - 400 EVIDENCE_FILE_EMPTY: 빈 파일이거나 공백뿐인 파일
          - 400 EVIDENCE_EXTENSION_NOT_ALLOWED: 확장자가 .md, .txt가 아님
          - 400 EVIDENCE_NOT_UTF8: UTF-8이 아니거나 NUL 문자를 포함함
          - 400 INVALID_REQUEST: file, handoverPackageId, documentType 누락 또는 형식 오류
          - 403 ACCESS_DENIED: 전임자가 아니거나 남의 Package
          - 404 RESOURCE_NOT_FOUND: Package가 없음
          - 409 STATE_CONFLICT: PUBLISHED이거나 이미 분석이 시작된 Package
          - 413 EVIDENCE_FILE_TOO_LARGE: 파일이 크기 상한(기본 128KB)을 넘음
          """)
  ResponseEntity<EvidenceDocumentResponse> upload(
      AuthenticatedMember requester, UUID handoverPackageId, DocumentType documentType, MultipartFile file);

  @Operation(
      summary = "Evidence 문서 목록 조회",
      description = """
          Package의 문서 메타 목록을 업로드 순으로 반환한다(원문 제외). 전임자는 자신의 Package만, 승인자는 전체를 본다.

          실패 케이스
          - 400 INVALID_REQUEST: handoverPackageId 누락 또는 UUID 형식 오류
          - 403 ACCESS_DENIED: 권한 없음 또는 남의 Package
          - 404 RESOURCE_NOT_FOUND: Package가 없음
          """)
  ResponseEntity<List<EvidenceDocumentResponse>> list(AuthenticatedMember requester, UUID handoverPackageId);

  @Operation(
      summary = "Evidence 문서 단건 조회",
      description = """
          문서 메타와 정규화된 원문(content)을 반환한다. 전임자는 자신의 Package 문서만, 승인자는 전체를 본다.

          실패 케이스
          - 400 INVALID_REQUEST: 경로의 ID가 UUID 형식이 아님
          - 403 ACCESS_DENIED: 권한 없음 또는 남의 Package 문서
          - 404 RESOURCE_NOT_FOUND: 문서가 없음
          """)
  ResponseEntity<EvidenceDocumentDetailResponse> get(AuthenticatedMember requester, UUID evidenceDocumentId);
}
