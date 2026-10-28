package com.chuseok22.whoim.server.card.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAccessDeniedHandler;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAuthenticationEntryPoint;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtProvider;
import com.chuseok22.whoim.server.auth.infrastructure.config.SecurityConfig;
import com.chuseok22.whoim.server.card.application.dto.response.CardDetailResponse;
import com.chuseok22.whoim.server.card.application.dto.response.CardEvidenceLinkResponse;
import com.chuseok22.whoim.server.card.application.dto.response.CardStateHistoryResponse;
import com.chuseok22.whoim.server.card.application.dto.response.CardSummaryResponse;
import com.chuseok22.whoim.server.card.application.service.CardReviewService;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.common.application.exception.GlobalExceptionHandler;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = CardReviewController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "jwt.secret-key=security-slice-dummy-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz-ABCDEFG",
    "jwt.access-exp-millis=3600000",
    "jwt.issuer=test-issuer"
})
// Docs 인터페이스 + @PreAuthorize 조합이 JDK 프록시가 되어 슬라이스에서 매핑이 사라지는 것을 막는다
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({
    SecurityConfig.class,
    JwtProvider.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class,
    GlobalExceptionHandler.class,
    CardReviewControllerTest.FixedClockConfig.class
})
class CardReviewControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-17T00:00:00Z");
  private static final String CARDS = "/api/cards";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @MockitoBean
  private CardReviewService cardReviewService;

  @Test
  void list_asPredecessorAndReviewer_returns200() throws Exception {
    given(cardReviewService.list(any(), any())).willReturn(List.of());

    mockMvc.perform(authorized(get(CARDS), Role.USER, Position.PREDECESSOR)).andExpect(status().isOk());
    mockMvc.perform(authorized(get(CARDS), Role.USER, Position.REVIEWER)).andExpect(status().isOk());
    mockMvc.perform(authorized(get(CARDS + "?status=DRAFT"), Role.USER, Position.REVIEWER))
        .andExpect(status().isOk());
  }

  @Test
  void list_withUnknownStatus_returns400() throws Exception {
    mockMvc.perform(authorized(get(CARDS + "?status=BOGUS"), Role.USER, Position.REVIEWER))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(cardReviewService);
  }

  @Test
  void listAndGet_asAdmin_returns403() throws Exception {
    String cardPath = CARDS + "/" + UUID.randomUUID();

    mockMvc.perform(authorized(get(CARDS), Role.ADMIN, null)).andExpect(status().isForbidden());
    mockMvc.perform(authorized(get(cardPath), Role.ADMIN, null)).andExpect(status().isForbidden());

    verifyNoInteractions(cardReviewService);
  }

  @Test
  void listAndGet_asSuccessor_returns200() throws Exception {
    given(cardReviewService.list(any(), any())).willReturn(List.of());
    given(cardReviewService.get(any(), any())).willReturn(emptyDetail());

    mockMvc.perform(authorized(get(CARDS), Role.USER, Position.SUCCESSOR)).andExpect(status().isOk());
    mockMvc.perform(authorized(get(CARDS + "/" + UUID.randomUUID()), Role.USER, Position.SUCCESSOR))
        .andExpect(status().isOk());
  }

  @Test
  void get_asPredecessor_returns200() throws Exception {
    given(cardReviewService.get(any(), any())).willReturn(emptyDetail());

    mockMvc.perform(authorized(get(CARDS + "/" + UUID.randomUUID()), Role.USER, Position.PREDECESSOR))
        .andExpect(status().isOk());
  }

  @Test
  void list_exposesActionAndEvidenceCounts() throws Exception {
    given(cardReviewService.list(any(), any())).willReturn(List.of(new CardSummaryResponse(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", CardStatus.APPROVED,
        "재시도 5회 유지", 2, 1, null)));

    mockMvc.perform(authorized(get(CARDS), Role.USER, Position.REVIEWER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].action").value("재시도 5회 유지"))
        .andExpect(jsonPath("$[0].directEvidenceCount").value(2))
        .andExpect(jsonPath("$[0].recordedEvidenceCount").value(1));
  }

  @Test
  void get_exposesEvidenceElementsAndActorName() throws Exception {
    given(cardReviewService.get(any(), any())).willReturn(new CardDetailResponse(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CardStatus.APPROVED, "제목",
        null, null, null, null, null, null, null, null, null, null,
        List.of(new CardEvidenceLinkResponse(
            EvidenceLinkType.DIRECT, 2, null, null, null, null, "발언", List.of(TraceElement.JUDGMENT))),
        List.of(new CardStateHistoryResponse(
            CardStatus.DRAFT, CardStatus.PREDECESSOR_CONFIRMED, "login-id", "홍길동", null, null)),
        null, null));

    mockMvc.perform(authorized(get(CARDS + "/" + UUID.randomUUID()), Role.USER, Position.PREDECESSOR))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.evidenceLinks[0].elements[0]").value("JUDGMENT"))
        .andExpect(jsonPath("$.stateHistory[0].actorName").value("홍길동"));
  }

  @Test
  void confirm_asPredecessor_returns200() throws Exception {
    given(cardReviewService.confirm(any(), any())).willReturn(emptyDetail());

    mockMvc.perform(authorized(post(cardAction("confirm")), Role.USER, Position.PREDECESSOR))
        .andExpect(status().isOk());
  }

  @Test
  void confirm_asOthers_returns403() throws Exception {
    mockMvc.perform(authorized(post(cardAction("confirm")), Role.USER, Position.REVIEWER))
        .andExpect(status().isForbidden());
    mockMvc.perform(authorized(post(cardAction("confirm")), Role.USER, Position.SUCCESSOR))
        .andExpect(status().isForbidden());
    mockMvc.perform(authorized(post(cardAction("confirm")), Role.ADMIN, null)).andExpect(status().isForbidden());

    verifyNoInteractions(cardReviewService);
  }

  @Test
  void approve_asReviewer_returns200() throws Exception {
    given(cardReviewService.approve(any(), any())).willReturn(emptyDetail());

    mockMvc.perform(authorized(post(cardAction("approve")), Role.USER, Position.REVIEWER))
        .andExpect(status().isOk());
  }

  @Test
  void approve_asOthers_returns403() throws Exception {
    mockMvc.perform(authorized(post(cardAction("approve")), Role.USER, Position.PREDECESSOR))
        .andExpect(status().isForbidden());
    mockMvc.perform(authorized(post(cardAction("approve")), Role.USER, Position.SUCCESSOR))
        .andExpect(status().isForbidden());
    mockMvc.perform(authorized(post(cardAction("approve")), Role.ADMIN, null)).andExpect(status().isForbidden());

    verifyNoInteractions(cardReviewService);
  }

  @Test
  void reject_asReviewer_returns200() throws Exception {
    given(cardReviewService.reject(any(), any(), any())).willReturn(emptyDetail());

    mockMvc.perform(authorized(post(cardAction("reject")), Role.USER, Position.REVIEWER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"reason": "근거가 부족합니다"}
                """))
        .andExpect(status().isOk());
  }

  @Test
  void reject_withBlankOrMissingReason_returns400() throws Exception {
    for (String body : new String[] {"{\"reason\": \"  \"}", "{}"}) {
      mockMvc.perform(authorized(post(cardAction("reject")), Role.USER, Position.REVIEWER)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isBadRequest());
    }

    verifyNoInteractions(cardReviewService);
  }

  @Test
  void reject_withoutBody_returns400() throws Exception {
    mockMvc.perform(authorized(post(cardAction("reject")), Role.USER, Position.REVIEWER))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(cardReviewService);
  }

  @Test
  void reject_withTooLongReason_returns400() throws Exception {
    String body = "{\"reason\": \"" + "가".repeat(501) + "\"}";

    mockMvc.perform(authorized(post(cardAction("reject")), Role.USER, Position.REVIEWER)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(cardReviewService);
  }

  @Test
  void reject_asOthers_returns403() throws Exception {
    String body = "{\"reason\": \"사유\"}";
    for (Position position : new Position[] {Position.PREDECESSOR, Position.SUCCESSOR}) {
      mockMvc.perform(authorized(post(cardAction("reject")), Role.USER, position)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isForbidden());
    }
    mockMvc.perform(authorized(post(cardAction("reject")), Role.ADMIN, null)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden());

    verifyNoInteractions(cardReviewService);
  }

  @Test
  void allEndpoints_withoutToken_return401() throws Exception {
    mockMvc.perform(get(CARDS)).andExpect(status().isUnauthorized());
    mockMvc.perform(get(CARDS + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    mockMvc.perform(post(cardAction("confirm"))).andExpect(status().isUnauthorized());
    mockMvc.perform(post(cardAction("approve"))).andExpect(status().isUnauthorized());
    mockMvc.perform(post(cardAction("reject"))).andExpect(status().isUnauthorized());
  }

  private static String cardAction(String action) {
    return CARDS + "/" + UUID.randomUUID() + "/" + action;
  }

  private static CardDetailResponse emptyDetail() {
    return new CardDetailResponse(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CardStatus.DRAFT, "제목",
        null, null, null, null, null, null, null, null, null, null, List.of(), List.of(), null, null);
  }

  private MockHttpServletRequestBuilder authorized(
      MockHttpServletRequestBuilder builder, Role role, Position position) {
    String token = jwtProvider.issue(new AuthenticatedMember(UUID.randomUUID(), "probe-user", role, position));
    return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
