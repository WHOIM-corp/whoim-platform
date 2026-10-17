package com.chuseok22.whoim.server.consultation.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAccessDeniedHandler;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAuthenticationEntryPoint;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtProvider;
import com.chuseok22.whoim.server.auth.infrastructure.config.SecurityConfig;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.common.application.exception.GlobalExceptionHandler;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationQuestionsResponse;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationSuggestionsResponse;
import com.chuseok22.whoim.server.consultation.application.service.ConsultationService;
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

@WebMvcTest(controllers = ConsultationController.class)
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
    ConsultationControllerTest.FixedClockConfig.class
})
class ConsultationControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-17T00:00:00Z");
  private static final String QUESTIONS = "/api/consultations/questions";
  private static final String SUGGESTIONS = "/api/consultations/suggestions";
  private static final String SITUATION_BODY = "{\"situation\": \"결제 성공률이 93%로 떨어졌어\"}";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @MockitoBean
  private ConsultationService consultationService;

  @Test
  void questions_asSuccessor_returns200() throws Exception {
    given(consultationService.questions(any(), any()))
        .willReturn(new ConsultationQuestionsResponse(List.of("정산 배치가 실행 중인가요?"), null));

    mockMvc.perform(json(post(QUESTIONS), Role.USER, Position.SUCCESSOR, SITUATION_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.questions[0]").value("정산 배치가 실행 중인가요?"));
  }

  @Test
  void suggestions_asSuccessor_returns200() throws Exception {
    given(consultationService.suggest(any(), any()))
        .willReturn(new ConsultationSuggestionsResponse(List.of(), "관련 Card가 없습니다."));

    mockMvc.perform(json(post(SUGGESTIONS), Role.USER, Position.SUCCESSOR, """
            {"situation": "결제 성공률이 93%로 떨어졌어",
             "answers": [{"question": "정산 배치가 실행 중인가요?", "answer": "아니요"}]}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("관련 Card가 없습니다."));
  }

  @Test
  void suggestions_withoutAnswersField_returns200() throws Exception {
    given(consultationService.suggest(any(), any()))
        .willReturn(new ConsultationSuggestionsResponse(List.of(), null));

    mockMvc.perform(json(post(SUGGESTIONS), Role.USER, Position.SUCCESSOR, SITUATION_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void bothEndpoints_asOthers_return403() throws Exception {
    for (String path : new String[] {QUESTIONS, SUGGESTIONS}) {
      for (Position position : new Position[] {Position.PREDECESSOR, Position.REVIEWER}) {
        mockMvc.perform(json(post(path), Role.USER, position, SITUATION_BODY))
            .andExpect(status().isForbidden());
      }
      mockMvc.perform(json(post(path), Role.ADMIN, null, SITUATION_BODY)).andExpect(status().isForbidden());
    }

    verifyNoInteractions(consultationService);
  }

  @Test
  void bothEndpoints_withoutToken_return401() throws Exception {
    for (String path : new String[] {QUESTIONS, SUGGESTIONS}) {
      mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(SITUATION_BODY))
          .andExpect(status().isUnauthorized());
    }
  }

  @Test
  void bothEndpoints_withBlankMissingOrTooLongSituation_return400() throws Exception {
    String tooLong = "{\"situation\": \"" + "가".repeat(2001) + "\"}";
    for (String path : new String[] {QUESTIONS, SUGGESTIONS}) {
      for (String body : new String[] {"{\"situation\": \"  \"}", "{}", tooLong}) {
        mockMvc.perform(json(post(path), Role.USER, Position.SUCCESSOR, body))
            .andExpect(status().isBadRequest());
      }
    }

    verifyNoInteractions(consultationService);
  }

  @Test
  void suggestions_withInvalidAnswers_returns400() throws Exception {
    String fourAnswers = "{\"situation\": \"상황\", \"answers\": ["
        + "{\"question\": \"q\", \"answer\": \"a\"},{\"question\": \"q\", \"answer\": \"a\"},"
        + "{\"question\": \"q\", \"answer\": \"a\"},{\"question\": \"q\", \"answer\": \"a\"}]}";
    String[] invalidBodies = {
        fourAnswers,
        "{\"situation\": \"상황\", \"answers\": [null]}",
        "{\"situation\": \"상황\", \"answers\": [{\"question\": \"q\", \"answer\": \" \"}]}",
        "{\"situation\": \"상황\", \"answers\": [{\"answer\": \"a\"}]}"
    };
    for (String body : invalidBodies) {
      mockMvc.perform(json(post(SUGGESTIONS), Role.USER, Position.SUCCESSOR, body))
          .andExpect(status().isBadRequest());
    }

    verifyNoInteractions(consultationService);
  }

  @Test
  void questions_whenLlmUnavailable_returns503() throws Exception {
    given(consultationService.questions(any(), any())).willThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));

    mockMvc.perform(json(post(QUESTIONS), Role.USER, Position.SUCCESSOR, SITUATION_BODY))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.errorCode").value("LLM_UNAVAILABLE"));
  }

  private MockHttpServletRequestBuilder json(
      MockHttpServletRequestBuilder builder, Role role, Position position, String body) {
    String token = jwtProvider.issue(new AuthenticatedMember(UUID.randomUUID(), "probe-user", role, position));
    return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
