package com.chuseok22.whoim.server.interview.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.interview.application.dto.request.InterviewAnswerRequest;
import com.chuseok22.whoim.server.interview.application.dto.request.InterviewStartRequest;
import com.chuseok22.whoim.server.interview.application.dto.response.InterviewResponse;
import com.chuseok22.whoim.server.interview.application.service.InterviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 요청 본문에 전임자의 답변이 담기므로 @LogMonitoring을 붙이지 않는다
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController implements InterviewControllerDocs {

  private final InterviewService interviewService;

  @Override
  @PreAuthorize("hasAuthority('POSITION_PREDECESSOR')")
  @PostMapping
  public ResponseEntity<InterviewResponse> start(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @Valid @RequestBody InterviewStartRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(interviewService.start(requester, request));
  }

  @Override
  @PreAuthorize("hasAuthority('POSITION_PREDECESSOR')")
  @PostMapping("/{interview-id}/answers")
  public ResponseEntity<InterviewResponse> submitAnswer(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("interview-id") UUID interviewId,
      @Valid @RequestBody InterviewAnswerRequest request) {
    return ResponseEntity.ok(interviewService.submitAnswer(requester, interviewId, request));
  }

  @Override
  @PreAuthorize("hasAuthority('POSITION_PREDECESSOR')")
  @PostMapping("/{interview-id}/retry")
  public ResponseEntity<InterviewResponse> retry(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("interview-id") UUID interviewId) {
    return ResponseEntity.ok(interviewService.retry(requester, interviewId));
  }

  @Override
  @PreAuthorize("hasAnyAuthority('POSITION_PREDECESSOR','POSITION_REVIEWER')")
  @GetMapping("/{interview-id}")
  public ResponseEntity<InterviewResponse> get(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("interview-id") UUID interviewId) {
    return ResponseEntity.ok(interviewService.get(requester, interviewId));
  }
}
