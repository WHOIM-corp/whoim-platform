package com.chuseok22.whoim.server.card.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAccessDeniedHandler;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAuthenticationEntryPoint;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtProvider;
import com.chuseok22.whoim.server.auth.infrastructure.config.SecurityConfig;
import com.chuseok22.whoim.server.card.application.dto.response.CardDetailResponse;
import com.chuseok22.whoim.server.card.application.service.CardReviewService;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.common.application.exception.GlobalExceptionHandler;
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
  void listAndGet_asSuccessorOrAdmin_returns403() throws Exception {
    String cardPath = CARDS + "/" + UUID.randomUUID();

    mockMvc.perform(authorized(get(CARDS), Role.USER, Position.SUCCESSOR)).andExpect(status().isForbidden());
    mockMvc.perform(authorized(get(CARDS), Role.ADMIN, null)).andExpect(status().isForbidden());
    mockMvc.perform(authorized(get(cardPath), Role.USER, Position.SUCCESSOR)).andExpect(status().isForbidden());
    mockMvc.perform(authorized(get(cardPath), Role.ADMIN, null)).andExpect(status().isForbidden());

    verifyNoInteractions(cardReviewService);
  }

  @Test
  void get_asPredecessor_returns200() throws Exception {
    given(cardReviewService.get(any(), any())).willReturn(emptyDetail());

    mockMvc.perform(authorized(get(CARDS + "/" + UUID.randomUUID()), Role.USER, Position.PREDECESSOR))
        .andExpect(status().isOk());
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
  void allEndpoints_withoutToken_return401() throws Exception {
    mockMvc.perform(get(CARDS)).andExpect(status().isUnauthorized());
    mockMvc.perform(get(CARDS + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    mockMvc.perform(post(cardAction("confirm"))).andExpect(status().isUnauthorized());
    mockMvc.perform(post(cardAction("approve"))).andExpect(status().isUnauthorized());
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
