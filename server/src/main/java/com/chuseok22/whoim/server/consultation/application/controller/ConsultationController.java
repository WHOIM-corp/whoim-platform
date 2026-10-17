package com.chuseok22.whoim.server.consultation.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.consultation.application.dto.request.ConsultationQuestionsRequest;
import com.chuseok22.whoim.server.consultation.application.dto.request.ConsultationSuggestionsRequest;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationQuestionsResponse;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationSuggestionsResponse;
import com.chuseok22.whoim.server.consultation.application.service.ConsultationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// 요청에 후임자가 쓴 상황이, 응답에 전임자 발언 발췌가 담기므로 @LogMonitoring을 붙이지 않는다
@RestController
@RequiredArgsConstructor
public class ConsultationController implements ConsultationControllerDocs {

  private final ConsultationService consultationService;

  @Override
  @PreAuthorize("hasAuthority('POSITION_SUCCESSOR')")
  @PostMapping("/api/consultations/questions")
  public ResponseEntity<ConsultationQuestionsResponse> questions(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @Valid @RequestBody ConsultationQuestionsRequest request) {
    return ResponseEntity.ok(consultationService.questions(requester, request));
  }

  @Override
  @PreAuthorize("hasAuthority('POSITION_SUCCESSOR')")
  @PostMapping("/api/consultations/suggestions")
  public ResponseEntity<ConsultationSuggestionsResponse> suggestions(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @Valid @RequestBody ConsultationSuggestionsRequest request) {
    return ResponseEntity.ok(consultationService.suggest(requester, request));
  }
}
