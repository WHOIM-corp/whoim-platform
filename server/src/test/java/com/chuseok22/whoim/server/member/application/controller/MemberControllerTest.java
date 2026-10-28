package com.chuseok22.whoim.server.member.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAccessDeniedHandler;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAuthenticationEntryPoint;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtProvider;
import com.chuseok22.whoim.server.auth.infrastructure.config.SecurityConfig;
import com.chuseok22.whoim.server.common.application.exception.GlobalExceptionHandler;
import com.chuseok22.whoim.server.member.application.dto.response.MemberSearchResponse;
import com.chuseok22.whoim.server.member.application.service.MemberService;
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

@WebMvcTest(controllers = MemberController.class)
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
    MemberControllerTest.FixedClockConfig.class
})
class MemberControllerTest {

  private static final String BASE_URL = "/api/members";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @MockitoBean
  private MemberService memberService;

  @Test
  void list_asReviewer_returnsProfileWithoutUsername() throws Exception {
    given(memberService.search(eq(Position.SUCCESSOR), any(), eq(6))).willReturn(List.of(
        new MemberSearchResponse(UUID.randomUUID(), "후임자", "A-001", "개발팀", "선임", Position.SUCCESSOR)));

    mockMvc.perform(get(BASE_URL).param("position", "SUCCESSOR").param("keyword", "후")
            .header(HttpHeaders.AUTHORIZATION, bearer(Position.REVIEWER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].employeeNumber").value("A-001"))
        .andExpect(jsonPath("$[0].department").value("개발팀"))
        .andExpect(jsonPath("$[0].jobTitle").value("선임"))
        .andExpect(jsonPath("$[0].username").doesNotExist());
  }

  @Test
  void list_withoutLimit_usesDefaultSix() throws Exception {
    given(memberService.search(any(), any(), eq(6))).willReturn(List.of());

    mockMvc.perform(get(BASE_URL).param("position", "SUCCESSOR")
            .header(HttpHeaders.AUTHORIZATION, bearer(Position.REVIEWER)))
        .andExpect(status().isOk());

    verify(memberService).search(eq(Position.SUCCESSOR), any(), eq(6));
  }

  @Test
  void list_asPredecessor_returns403() throws Exception {
    mockMvc.perform(get(BASE_URL).param("position", "SUCCESSOR")
            .header(HttpHeaders.AUTHORIZATION, bearer(Position.PREDECESSOR)))
        .andExpect(status().isForbidden());
  }

  @Test
  void list_withoutPosition_returns400() throws Exception {
    mockMvc.perform(get(BASE_URL).header(HttpHeaders.AUTHORIZATION, bearer(Position.REVIEWER)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void list_withInvalidPosition_returns400() throws Exception {
    mockMvc.perform(get(BASE_URL).param("position", "NOBODY")
            .header(HttpHeaders.AUTHORIZATION, bearer(Position.REVIEWER)))
        .andExpect(status().isBadRequest());
  }

  private String bearer(Position position) {
    return "Bearer " + jwtProvider.issue(new AuthenticatedMember(UUID.randomUUID(), "probe-user", Role.USER, position));
  }

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"), ZoneOffset.UTC);
    }
  }
}
