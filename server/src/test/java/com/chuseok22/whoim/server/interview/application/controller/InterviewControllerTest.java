package com.chuseok22.whoim.server.interview.application.controller;

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
import com.chuseok22.whoim.server.interview.application.dto.response.InterviewResponse;
import com.chuseok22.whoim.server.interview.application.service.InterviewService;
import com.chuseok22.whoim.server.interview.core.constant.InterviewStatus;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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

@WebMvcTest(controllers = InterviewController.class)
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
    InterviewControllerTest.FixedClockConfig.class
})
class InterviewControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-17T00:00:00Z");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @MockitoBean
  private InterviewService interviewService;

  @Test
  void start_asPredecessor_returns201() throws Exception {
    given(interviewService.start(any(), any())).willReturn(responseOf());

    mockMvc.perform(startRequest(Role.USER, Position.PREDECESSOR, "{\"gapId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void start_asReviewerSuccessorAdmin_returns403() throws Exception {
    String body = "{\"gapId\":\"" + UUID.randomUUID() + "\"}";

    mockMvc.perform(startRequest(Role.USER, Position.REVIEWER, body))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    mockMvc.perform(startRequest(Role.USER, Position.SUCCESSOR, body)).andExpect(status().isForbidden());
    mockMvc.perform(startRequest(Role.ADMIN, null, body)).andExpect(status().isForbidden());

    verifyNoInteractions(interviewService);
  }

  @Test
  void start_missingGapId_returns400() throws Exception {
    mockMvc.perform(startRequest(Role.USER, Position.PREDECESSOR, "{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
  }

  @Test
  void answer_asReviewer_returns403() throws Exception {
    mockMvc.perform(answerRequest(Role.USER, Position.REVIEWER, "{\"answer\":\"답변\"}"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(interviewService);
  }

  @Test
  void answer_blank_returns400() throws Exception {
    mockMvc.perform(answerRequest(Role.USER, Position.PREDECESSOR, "{\"answer\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    mockMvc.perform(answerRequest(Role.USER, Position.PREDECESSOR, "{}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(interviewService);
  }

  @Test
  void answer_over4000Characters_returns400() throws Exception {
    mockMvc.perform(answerRequest(Role.USER, Position.PREDECESSOR, "{\"answer\":\"" + "가".repeat(4001) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

    verifyNoInteractions(interviewService);
  }

  @Test
  void answer_exactly4000Characters_returns200() throws Exception {
    given(interviewService.submitAnswer(any(), any(), any())).willReturn(responseOf());

    mockMvc.perform(answerRequest(Role.USER, Position.PREDECESSOR, "{\"answer\":\"" + "가".repeat(4000) + "\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void retry_onlyPredecessor() throws Exception {
    given(interviewService.retry(any(), any())).willReturn(responseOf());

    mockMvc.perform(post("/api/interviews/" + UUID.randomUUID() + "/retry")
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, Position.PREDECESSOR)))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/interviews/" + UUID.randomUUID() + "/retry")
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, Position.REVIEWER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void get_predecessorAndReviewerAllowed_successorAdminDenied() throws Exception {
    given(interviewService.get(any(), any())).willReturn(responseOf());

    for (Position position : List.of(Position.PREDECESSOR, Position.REVIEWER)) {
      mockMvc.perform(getRequest(Role.USER, position)).andExpect(status().isOk());
    }
    mockMvc.perform(getRequest(Role.USER, Position.SUCCESSOR)).andExpect(status().isForbidden());
    mockMvc.perform(getRequest(Role.ADMIN, null)).andExpect(status().isForbidden());
  }

  @Test
  void noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/interviews/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
  }

  private InterviewResponse responseOf() {
    return new InterviewResponse(UUID.randomUUID(), UUID.randomUUID(), InterviewStatus.IN_PROGRESS, null, "질문", List.of(), Map.of());
  }

  private MockHttpServletRequestBuilder startRequest(Role role, Position position, String body) {
    return post("/api/interviews")
        .header(HttpHeaders.AUTHORIZATION, bearer(role, position))
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  private MockHttpServletRequestBuilder answerRequest(Role role, Position position, String body) {
    return post("/api/interviews/" + UUID.randomUUID() + "/answers")
        .header(HttpHeaders.AUTHORIZATION, bearer(role, position))
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  private MockHttpServletRequestBuilder getRequest(Role role, Position position) {
    return get("/api/interviews/" + UUID.randomUUID())
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
