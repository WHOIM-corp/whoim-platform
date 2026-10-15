package com.chuseok22.whoim.server.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.whoim.server.auth.application.controller.AuthController;
import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.auth.application.service.LoginService;
import com.chuseok22.whoim.server.auth.infrastructure.properties.JwtProperties;
import com.chuseok22.whoim.server.common.application.exception.GlobalExceptionHandler;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = {AuthController.class, AuthSecurityTest.ProbeController.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "jwt.secret-key=security-slice-dummy-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz-ABCDEFG",
    "jwt.access-exp-millis=3600000",
    "jwt.issuer=test-issuer"
})
@Import({
    SecurityConfig.class,
    JwtProvider.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class,
    LoginService.class,
    GlobalExceptionHandler.class,
    AuthSecurityTest.ProbeController.class,
    AuthSecurityTest.FixedClockConfig.class
})
class AuthSecurityTest {

  private static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");
  private static final String DUMMY_PASSWORD = "dummy-login-pw-4c2e";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private JwtProperties jwtProperties;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private MemberRepository memberRepository;

  @Test
  void noToken_returns401Unauthenticated() throws Exception {
    mockMvc.perform(get("/probe/authenticated"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
  }

  @Test
  void invalidToken_returns401Unauthenticated() throws Exception {
    mockMvc.perform(get("/probe/authenticated").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
  }

  @Test
  void expiredToken_returns401ExpiredToken() throws Exception {
    JwtProvider pastProvider = new JwtProvider(jwtProperties, Clock.fixed(NOW.minus(Duration.ofDays(1)), ZoneOffset.UTC));
    String expiredToken = pastProvider.issue(member(Role.USER, Position.REVIEWER));

    mockMvc.perform(get("/probe/authenticated").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("EXPIRED_TOKEN"));
  }

  @Test
  void insufficientPosition_returns403AccessDeniedJson() throws Exception {
    String token = jwtProvider.issue(member(Role.USER, Position.SUCCESSOR));

    mockMvc.perform(get("/probe/reviewer-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
  }

  @Test
  void adminWithoutPosition_isDeniedBusinessEndpoint() throws Exception {
    String token = jwtProvider.issue(member(Role.ADMIN, null));

    mockMvc.perform(get("/probe/reviewer-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
  }

  @Test
  void matchingPosition_isAllowed() throws Exception {
    String token = jwtProvider.issue(member(Role.USER, Position.REVIEWER));

    mockMvc.perform(get("/probe/reviewer-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void validToken_exposesPrincipal() throws Exception {
    AuthenticatedMember member = member(Role.USER, Position.PREDECESSOR);
    String token = jwtProvider.issue(member);

    mockMvc.perform(get("/probe/principal").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.memberId").value(member.memberId().toString()))
        .andExpect(jsonPath("$.username").value("probe-user"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.position").value("PREDECESSOR"));
  }

  @Test
  void login_success_returnsTokenAndMember() throws Exception {
    givenStoredMember();

    mockMvc.perform(loginRequest("probe-user", DUMMY_PASSWORD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.member.memberId").isNotEmpty())
        .andExpect(jsonPath("$.member.id").doesNotExist())
        .andExpect(jsonPath("$.member.username").value("probe-user"))
        .andExpect(jsonPath("$.member.role").value("USER"))
        .andExpect(jsonPath("$.member.position").value("SUCCESSOR"))
        .andExpect(jsonPath("$.member.password").doesNotExist());
  }

  @Test
  void login_unknownUsernameAndWrongPassword_returnIdenticalBody() throws Exception {
    givenStoredMember();
    given(memberRepository.findByUsername("ghost")).willReturn(Optional.empty());

    String unknownBody = mockMvc.perform(loginRequest("ghost", DUMMY_PASSWORD))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
        .andReturn().getResponse().getContentAsString();
    String wrongPasswordBody = mockMvc.perform(loginRequest("probe-user", "wrong-pw"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
        .andReturn().getResponse().getContentAsString();

    assertThat(unknownBody).isEqualTo(wrongPasswordBody);
  }

  @Test
  void login_blankValues_returns400InvalidRequest() throws Exception {
    mockMvc.perform(loginRequest("", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
  }

  @Test
  void login_isPublicEvenWithInvalidAuthorizationHeader() throws Exception {
    givenStoredMember();

    mockMvc.perform(loginRequest("probe-user", DUMMY_PASSWORD).header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());
  }

  @Test
  void cors_preflightFromAllowedOrigins_passes() throws Exception {
    for (String origin : CorsOrigins.ALLOWED_ORIGINS) {
      mockMvc.perform(preflight(origin))
          .andExpect(status().is2xxSuccessful())
          .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
    }
  }

  @Test
  void cors_preflightFromUnknownOrigin_isForbidden() throws Exception {
    mockMvc.perform(preflight("https://evil.example.com"))
        .andExpect(status().isForbidden());
  }

  @Test
  void actuatorHealth_isPublic() throws Exception {
    int status = mockMvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus();

    assertThat(status).isNotIn(401, 403);
  }

  @Test
  void unknownPath_authenticated_returns404ResourceNotFound() throws Exception {
    String token = jwtProvider.issue(member(Role.USER, Position.REVIEWER));

    mockMvc.perform(get("/probe/none").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void unknownPath_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/probe/none"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
  }

  private void givenStoredMember() {
    Member stored = Member.builder()
        .username("probe-user")
        .password(passwordEncoder.encode(DUMMY_PASSWORD))
        .name("프로브")
        .role(Role.USER)
        .position(Position.SUCCESSOR)
        .build();
    ReflectionTestUtils.setField(stored, "id", UUID.randomUUID());
    given(memberRepository.findByUsername("probe-user")).willReturn(Optional.of(stored));
  }

  private MockHttpServletRequestBuilder loginRequest(String username, String password) {
    return post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
  }

  private MockHttpServletRequestBuilder preflight(String origin) {
    return options("/api/auth/login")
        .header(HttpHeaders.ORIGIN, origin)
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type");
  }

  private AuthenticatedMember member(Role role, Position position) {
    return new AuthenticatedMember(UUID.randomUUID(), "probe-user", role, position);
  }

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }

  @RestController
  static class ProbeController {

    @GetMapping("/probe/reviewer-only")
    @PreAuthorize("hasAuthority('POSITION_REVIEWER')")
    String reviewerOnly() {
      return "ok";
    }

    @GetMapping("/probe/authenticated")
    String authenticated() {
      return "ok";
    }

    @GetMapping("/probe/principal")
    AuthenticatedMember principal(@AuthenticationPrincipal AuthenticatedMember member) {
      return member;
    }
  }
}
