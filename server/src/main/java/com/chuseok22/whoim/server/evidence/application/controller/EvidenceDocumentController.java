package com.chuseok22.whoim.server.evidence.application.controller;

import com.chuseok22.logging.annotation.LogMonitoring;
import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.evidence.application.dto.response.EvidenceDocumentDetailResponse;
import com.chuseok22.whoim.server.evidence.application.dto.response.EvidenceDocumentResponse;
import com.chuseok22.whoim.server.evidence.application.service.EvidenceDocumentService;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/evidence-documents")
@RequiredArgsConstructor
public class EvidenceDocumentController implements EvidenceDocumentControllerDocs {

  private final EvidenceDocumentService evidenceDocumentService;

  @LogMonitoring
  @Override
  @PreAuthorize("hasAuthority('POSITION_PREDECESSOR')")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<EvidenceDocumentResponse> upload(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @RequestParam UUID handoverPackageId,
      @RequestParam DocumentType documentType,
      @RequestParam("file") MultipartFile file) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(evidenceDocumentService.upload(requester, handoverPackageId, documentType, file));
  }

  @LogMonitoring
  @Override
  @PreAuthorize("hasAnyAuthority('POSITION_PREDECESSOR','POSITION_REVIEWER')")
  @GetMapping
  public ResponseEntity<List<EvidenceDocumentResponse>> list(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @RequestParam UUID handoverPackageId) {
    return ResponseEntity.ok(evidenceDocumentService.list(requester, handoverPackageId));
  }

  // 응답에 최대 128KB 원문이 담기므로 @LogMonitoring을 붙이지 않는다
  @Override
  @PreAuthorize("hasAnyAuthority('POSITION_PREDECESSOR','POSITION_REVIEWER')")
  @GetMapping("/{evidence-document-id}")
  public ResponseEntity<EvidenceDocumentDetailResponse> get(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("evidence-document-id") UUID evidenceDocumentId) {
    return ResponseEntity.ok(evidenceDocumentService.get(requester, evidenceDocumentId));
  }
}
