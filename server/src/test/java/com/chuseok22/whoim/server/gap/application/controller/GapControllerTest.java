package com.chuseok22.whoim.server.gap.application.controller;

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
import com.chuseok22.whoim.server.common.application.exception.GlobalExceptionHandler;
import com.chuseok22.whoim.server.gap.application.dto.response.GapResponse;
import com.chuseok22.whoim.server.gap.application.service.GapAnalysisService;
import com.chuseok22.whoim.server.gap.application.service.GapService;
import com.chuseok22.whoim.server.gap.core.constant.GapProgress;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
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

@WebMvcTest(controllers = GapController.class)
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
    GapControllerTest.FixedClockConfig.class
})
class GapControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-17T00:00:00Z");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @MockitoBean
  private GapAnalysisService gapAnalysisService;

  @MockitoBean
  private GapService gapService;

  @Test
  void analyze_asPredecessor_returns200() throws Exception {
    given(gapAnalysisService.analyze(any(), any())).willReturn(List.of());

    mockMvc.perform(analysisRequest(Role.USER, Position.PREDECESSOR))
        .andExpect(status().isOk());
  }

  @Test
  void analyze_asReviewerSuccessorAdmin_returns403() throws Exception {
    mockMvc.perform(analysisRequest(Role.USER, Position.REVIEWER))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    mockMvc.perform(analysisRequest(Role.USER, Position.SUCCESSOR))
        .andExpect(status().isForbidden());
    mockMvc.perform(analysisRequest(Role.ADMIN, null))
        .andExpect(status().isForbidden());

    verifyNoInteractions(gapAnalysisService);
  }

  @Test
  void list_asPredecessorAndReviewer_returns200() throws Exception {
    UUID interviewId = UUID.randomUUID();
    UUID cardId = UUID.randomUUID();
    given(gapService.list(any(), any())).willReturn(List.of(new GapResponse(
        UUID.randomUUID(), GapType.CHOICE_GAP, "요약", "질문", List.of(), GapProgress.COMPLETED, interviewId, cardId)));

    for (Position position : List.of(Position.PREDECESSOR, Position.REVIEWER)) {
      mockMvc.perform(listRequest(Role.USER, position))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].interviewId").value(interviewId.toString()))
          .andExpect(jsonPath("$[0].cardId").value(cardId.toString()));
    }
  }

  @Test
  void list_asSuccessorAdmin_returns403() throws Exception {
    mockMvc.perform(listRequest(Role.USER, Position.SUCCESSOR))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    mockMvc.perform(listRequest(Role.ADMIN, null))
        .andExpect(status().isForbidden());

    verifyNoInteractions(gapService);
  }

  @Test
  void list_invalidOrMissingHandoverPackageId_returns400() throws Exception {
    String token = bearer(Role.USER, Position.REVIEWER);

    mockMvc.perform(get("/api/gaps").param("handoverPackageId", "not-a-uuid")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    mockMvc.perform(get("/api/gaps").header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void noToken_returns401() throws Exception {
    mockMvc.perform(post("/api/handover-packages/" + UUID.randomUUID() + "/gap-analysis"))
        .andExpect(status().isUnauthorized());
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder analysisRequest(Role role, Position position) {
    return post("/api/handover-packages/" + UUID.randomUUID() + "/gap-analysis")
        .header(HttpHeaders.AUTHORIZATION, bearer(role, position));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder listRequest(Role role, Position position) {
    return get("/api/gaps").param("handoverPackageId", UUID.randomUUID().toString())
        .header(HttpHeaders.AUTHORIZATION, bearer(role, position));
  }

  private String bearer(Role role, Position position) {
    return "Bearer " + jwtProvider.issue(new AuthenticatedMember(UUID.randomUUID(), "probe-user", role, position));
  }

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
