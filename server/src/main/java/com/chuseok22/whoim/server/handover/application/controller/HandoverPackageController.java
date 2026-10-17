package com.chuseok22.whoim.server.handover.application.controller;

import com.chuseok22.logging.annotation.LogMonitoring;
import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageCreateRequest;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageSuccessorRequest;
import com.chuseok22.whoim.server.handover.application.dto.response.HandoverPackageResponse;
import com.chuseok22.whoim.server.handover.application.service.HandoverPackageService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/handover-packages")
@RequiredArgsConstructor
public class HandoverPackageController implements HandoverPackageControllerDocs {

  private final HandoverPackageService handoverPackageService;

  @LogMonitoring
  @Override
  @PreAuthorize("hasAuthority('POSITION_REVIEWER')")
  @PostMapping
  public ResponseEntity<HandoverPackageResponse> create(@Valid @RequestBody HandoverPackageCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(handoverPackageService.create(request));
  }

  @LogMonitoring
  @Override
  @PreAuthorize("hasAuthority('POSITION_REVIEWER')")
  @PutMapping("/{handover-package-id}/successor")
  public ResponseEntity<HandoverPackageResponse> updateSuccessor(
      @PathVariable("handover-package-id") UUID handoverPackageId,
      @Valid @RequestBody HandoverPackageSuccessorRequest request) {
    return ResponseEntity.ok(handoverPackageService.updateSuccessor(handoverPackageId, request));
  }

  @LogMonitoring
  @Override
  @PreAuthorize("hasAnyAuthority('POSITION_PREDECESSOR','POSITION_REVIEWER')")
  @GetMapping
  public ResponseEntity<List<HandoverPackageResponse>> list(@AuthenticationPrincipal AuthenticatedMember requester) {
    return ResponseEntity.ok(handoverPackageService.list(requester));
  }

  @LogMonitoring
  @Override
  @PreAuthorize("hasAnyAuthority('POSITION_PREDECESSOR','POSITION_REVIEWER')")
  @GetMapping("/{handover-package-id}")
  public ResponseEntity<HandoverPackageResponse> get(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("handover-package-id") UUID handoverPackageId) {
    return ResponseEntity.ok(handoverPackageService.get(requester, handoverPackageId));
  }

  @LogMonitoring
  @Override
  @PreAuthorize("hasAuthority('POSITION_REVIEWER')")
  @PostMapping("/{handover-package-id}/publish")
  public ResponseEntity<HandoverPackageResponse> publish(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("handover-package-id") UUID handoverPackageId) {
    return ResponseEntity.ok(handoverPackageService.publish(requester, handoverPackageId));
  }
}
