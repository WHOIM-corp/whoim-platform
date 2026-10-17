package com.chuseok22.whoim.server.admin.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.chuseok22.whoim.server.admin.application.controller.AdminExceptionHandler;
import com.chuseok22.whoim.server.admin.application.controller.AdminLoginController;
import com.chuseok22.whoim.server.admin.application.service.AdminUserDetailsService;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAccessDeniedHandler;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAuthenticationEntryPoint;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtProvider;
import com.chuseok22.whoim.server.auth.infrastructure.config.SecurityConfig;
import com.chuseok22.whoim.server.common.application.exception.GlobalExceptionHandler;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.time.Clock;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = {AdminLoginController.class, AdminSecurityTest.ProbeController.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "jwt.secret-key=security-slice-dummy-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz-ABCDEFG",
    "jwt.access-exp-millis=3600000",
    "jwt.issuer=test-issuer"
})
@Import({
    SecurityConfig.class,
    AdminSecurityConfig.class,
    AdminLoginSuccessHandler.class,
    AdminUserDetailsService.class,
    JwtProvider.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class,
    GlobalExceptionHandler.class,
    AdminExceptionHandler.class,
    AdminSecurityTest.ProbeController.class,
    AdminSecurityTest.FixedClockConfig.class
})
class AdminSecurityTest {

  private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");
  private static final String ADMIN_USERNAME = "probe-admin";
  private static final String DUMMY_PASSWORD = "dummy-admin-pw-7d1f";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private MemberRepository memberRepository;

  @Test
  void unauthenticatedAdminPath_redirectsToLogin() throws Exception {
    mockMvc.perform(get("/api/admin/probe"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/admin/login")));
  }

  @Test
  void htmxRequestWithoutSession_returns401WithHxRedirect() throws Exception {
    mockMvc.perform(get("/api/admin/probe").header("HX-Request", "true"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("HX-Redirect", "/api/admin/login"));
  }

  @Test
  void adminSession_canAccessAdminPath() throws Exception {
    givenStoredMember(Role.ADMIN, null);
    MvcResult login = mockMvc.perform(loginRequest(ADMIN_USERNAME, DUMMY_PASSWORD)).andReturn();
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

    mockMvc.perform(get("/api/admin/probe").session(session))
        .andExpect(status().isOk());
  }

  @Test
  void loginPage_isPublic() throws Exception {
    mockMvc.perform(get("/api/admin/login"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/login"));
  }

  @Test
  void assets_arePublic() throws Exception {
    mockMvc.perform(get("/api/admin/assets/admin.css"))
        .andExpect(status().isOk());
  }

  @Test
  void adminLogin_succeeds_andSetsOneHourSession() throws Exception {
    givenStoredMember(Role.ADMIN, null);

    MvcResult result = mockMvc.perform(loginRequest(ADMIN_USERNAME, DUMMY_PASSWORD))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/api/admin/tables"))
        .andReturn();

    assertThat(result.getRequest().getSession().getMaxInactiveInterval()).isEqualTo(3600);
  }

  @Test
  void wrongPassword_redirectsToError() throws Exception {
    givenStoredMember(Role.ADMIN, null);

    mockMvc.perform(loginRequest(ADMIN_USERNAME, "wrong-pw"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/api/admin/login?error"));
  }

  @Test
  void unknownUsername_redirectsToError() throws Exception {
    given(memberRepository.findByUsername("ghost")).willReturn(Optional.empty());

    mockMvc.perform(loginRequest("ghost", DUMMY_PASSWORD))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/api/admin/login?error"));
  }

  @Test
  void userRoleMember_redirectsToError() throws Exception {
    givenStoredMember(Role.USER, Position.REVIEWER);

    mockMvc.perform(loginRequest(ADMIN_USERNAME, DUMMY_PASSWORD))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/api/admin/login?error"));
  }

  @Test
  void loginWithoutCsrf_returns403() throws Exception {
    givenStoredMember(Role.ADMIN, null);

    mockMvc.perform(post("/api/admin/login")
            .param("username", ADMIN_USERNAME)
            .param("password", DUMMY_PASSWORD))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminSession_isNotAcceptedOnApiChain() throws Exception {
    givenStoredMember(Role.ADMIN, null);
    MvcResult login = mockMvc.perform(loginRequest(ADMIN_USERNAME, DUMMY_PASSWORD)).andReturn();
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

    mockMvc.perform(get("/probe/authenticated").session(session))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
  }

  @Test
  void logout_redirectsToLogin() throws Exception {
    givenStoredMember(Role.ADMIN, null);
    MvcResult login = mockMvc.perform(loginRequest(ADMIN_USERNAME, DUMMY_PASSWORD)).andReturn();
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

    mockMvc.perform(post("/api/admin/logout").session(session).with(csrf()))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/api/admin/login?logout"));

    mockMvc.perform(get("/api/admin/probe").session(session))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/admin/login")));
  }

  @Test
  void settingSaveWithoutCsrf_returns403() throws Exception {
    givenStoredMember(Role.ADMIN, null);
    MvcResult login = mockMvc.perform(loginRequest(ADMIN_USERNAME, DUMMY_PASSWORD)).andReturn();
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

    mockMvc.perform(post("/api/admin/settings/llm.gemini.model").session(session).param("value", "x"))
        .andExpect(status().isForbidden());
  }

  private void givenStoredMember(Role role, Position position) {
    Member stored = Member.builder()
        .username(ADMIN_USERNAME)
        .password(passwordEncoder.encode(DUMMY_PASSWORD))
        .name("프로브 관리자")
        .role(role)
        .position(position)
        .build();
    ReflectionTestUtils.setField(stored, "id", UUID.randomUUID());
    given(memberRepository.findByUsername(ADMIN_USERNAME)).willReturn(Optional.of(stored));
  }

  private MockHttpServletRequestBuilder loginRequest(String username, String password) {
    return post("/api/admin/login")
        .with(csrf())
        .param("username", username)
        .param("password", password);
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

    @GetMapping("/api/admin/probe")
    String adminProbe() {
      return "ok";
    }

    @GetMapping("/probe/authenticated")
    String authenticated() {
      return "ok";
    }
  }
}
