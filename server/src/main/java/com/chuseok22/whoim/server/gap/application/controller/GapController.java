package com.chuseok22.whoim.server.gap.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.gap.application.dto.response.GapResponse;
import com.chuseok22.whoim.server.gap.application.service.GapAnalysisService;
import com.chuseok22.whoim.server.gap.application.service.GapService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 응답에 문서 원문 발췌가 담기므로 @LogMonitoring을 붙이지 않는다
@RestController
@RequiredArgsConstructor
public class GapController implements GapControllerDocs {

  private final GapAnalysisService gapAnalysisService;
  private final GapService gapService;

  @Override
  @PreAuthorize("hasAuthority('POSITION_PREDECESSOR')")
  @PostMapping("/api/handover-packages/{handover-package-id}/gap-analysis")
  public ResponseEntity<List<GapResponse>> analyze(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("handover-package-id") UUID handoverPackageId) {
    return ResponseEntity.ok(gapAnalysisService.analyze(requester, handoverPackageId));
  }

  @Override
  @PreAuthorize("hasAnyAuthority('POSITION_PREDECESSOR','POSITION_REVIEWER')")
  @GetMapping("/api/gaps")
  public ResponseEntity<List<GapResponse>> list(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @RequestParam UUID handoverPackageId) {
    return ResponseEntity.ok(gapService.list(requester, handoverPackageId));
  }
}
