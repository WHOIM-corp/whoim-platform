package com.chuseok22.whoim.server.card.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.application.dto.response.CardResponse;
import com.chuseok22.whoim.server.card.application.service.CardService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 응답에 전임자 답변 발췌가 담기므로 @LogMonitoring을 붙이지 않는다
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class CardController implements CardControllerDocs {

  private final CardService cardService;

  @Override
  @PreAuthorize("hasAuthority('POSITION_PREDECESSOR')")
  @PostMapping("/{interview-id}/card")
  public ResponseEntity<CardResponse> create(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("interview-id") UUID interviewId) {
    return ResponseEntity.status(HttpStatus.CREATED).body(cardService.create(requester, interviewId));
  }
}
