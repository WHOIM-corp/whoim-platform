package com.chuseok22.whoim.server.card.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAccessDeniedHandler;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAuthenticationEntryPoint;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtProvider;
import com.chuseok22.whoim.server.auth.infrastructure.config.SecurityConfig;
import com.chuseok22.whoim.server.card.application.dto.response.CardResponse;
import com.chuseok22.whoim.server.card.application.service.CardService;
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

@WebMvcTest(controllers = CardController.class)
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
    CardControllerTest.FixedClockConfig.class
})
class CardControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-17T00:00:00Z");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @MockitoBean
  private CardService cardService;

  @Test
  void create_asPredecessor_returns201() throws Exception {
    given(cardService.create(any(), any())).willReturn(new CardResponse(
        UUID.randomUUID(), UUID.randomUUID(), CardStatus.DRAFT, "제목",
        null, null, null, null, null, null, null, null, null, null, List.of(), null));

    mockMvc.perform(createRequest(Role.USER, Position.PREDECESSOR)).andExpect(status().isCreated());
  }

  @Test
  void create_asReviewerSuccessorAdmin_returns403() throws Exception {
    mockMvc.perform(createRequest(Role.USER, Position.REVIEWER)).andExpect(status().isForbidden());
    mockMvc.perform(createRequest(Role.USER, Position.SUCCESSOR)).andExpect(status().isForbidden());
    mockMvc.perform(createRequest(Role.ADMIN, null)).andExpect(status().isForbidden());

    verifyNoInteractions(cardService);
  }

  @Test
  void create_noToken_returns401() throws Exception {
    mockMvc.perform(post("/api/interviews/" + UUID.randomUUID() + "/card")).andExpect(status().isUnauthorized());
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createRequest(
      Role role, Position position) {
    String token = jwtProvider.issue(new AuthenticatedMember(UUID.randomUUID(), "probe-user", role, position));
    return post("/api/interviews/" + UUID.randomUUID() + "/card")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
