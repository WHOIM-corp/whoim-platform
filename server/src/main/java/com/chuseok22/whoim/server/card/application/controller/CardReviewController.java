package com.chuseok22.whoim.server.card.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.application.dto.request.CardRejectRequest;
import com.chuseok22.whoim.server.card.application.dto.response.CardDetailResponse;
import com.chuseok22.whoim.server.card.application.dto.response.CardSummaryResponse;
import com.chuseok22.whoim.server.card.application.service.CardReviewService;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 응답에 전임자 답변 발췌가 담기므로 @LogMonitoring을 붙이지 않는다
@RestController
@RequiredArgsConstructor
public class CardReviewController implements CardReviewControllerDocs {

  private final CardReviewService cardReviewService;

  @Override
  @PreAuthorize("hasAnyAuthority('POSITION_PREDECESSOR','POSITION_REVIEWER','POSITION_SUCCESSOR')")
  @GetMapping("/api/cards")
  public ResponseEntity<List<CardSummaryResponse>> list(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @RequestParam(value = "status", required = false) CardStatus status) {
    return ResponseEntity.ok(cardReviewService.list(requester, status));
  }

  @Override
  @PreAuthorize("hasAnyAuthority('POSITION_PREDECESSOR','POSITION_REVIEWER','POSITION_SUCCESSOR')")
  @GetMapping("/api/cards/{card-id}")
  public ResponseEntity<CardDetailResponse> get(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("card-id") UUID cardId) {
    return ResponseEntity.ok(cardReviewService.get(requester, cardId));
  }

  @Override
  @PreAuthorize("hasAuthority('POSITION_PREDECESSOR')")
  @PostMapping("/api/cards/{card-id}/confirm")
  public ResponseEntity<CardDetailResponse> confirm(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("card-id") UUID cardId) {
    return ResponseEntity.ok(cardReviewService.confirm(requester, cardId));
  }

  @Override
  @PreAuthorize("hasAuthority('POSITION_REVIEWER')")
  @PostMapping("/api/cards/{card-id}/approve")
  public ResponseEntity<CardDetailResponse> approve(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("card-id") UUID cardId) {
    return ResponseEntity.ok(cardReviewService.approve(requester, cardId));
  }

  @Override
  @PreAuthorize("hasAuthority('POSITION_REVIEWER')")
  @PostMapping("/api/cards/{card-id}/reject")
  public ResponseEntity<CardDetailResponse> reject(
      @AuthenticationPrincipal AuthenticatedMember requester,
      @PathVariable("card-id") UUID cardId,
      @Valid @RequestBody CardRejectRequest request) {
    return ResponseEntity.ok(cardReviewService.reject(requester, cardId, request));
  }
}
