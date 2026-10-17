package com.chuseok22.whoim.server.handover.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAccessDeniedHandler;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAuthenticationEntryPoint;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtProvider;
import com.chuseok22.whoim.server.auth.infrastructure.config.SecurityConfig;
import com.chuseok22.whoim.server.common.application.exception.GlobalExceptionHandler;
import com.chuseok22.whoim.server.handover.application.dto.response.HandoverPackageResponse;
import com.chuseok22.whoim.server.handover.application.service.HandoverPackageService;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.member.application.dto.response.MemberResponse;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HandoverPackageController.class)
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
    HandoverPackageControllerTest.FixedClockConfig.class
})
class HandoverPackageControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-17T00:00:00Z");
  private static final String BASE_URL = "/api/handover-packages";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @MockitoBean
  private HandoverPackageService handoverPackageService;

  @Test
  void create_asPredecessor_returns403() throws Exception {
    mockMvc.perform(createRequest(Position.PREDECESSOR, validBody()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

    verifyNoInteractions(handoverPackageService);
  }

  @Test
  void create_blankName_returns400() throws Exception {
    String body = "{\"predecessorId\":\"" + UUID.randomUUID() + "\",\"name\":\"  \"}";

    mockMvc.perform(createRequest(Position.REVIEWER, body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

    verifyNoInteractions(handoverPackageService);
  }

  @Test
  void create_asReviewer_returns201() throws Exception {
    given(handoverPackageService.create(any())).willReturn(response());

    mockMvc.perform(createRequest(Position.REVIEWER, validBody()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("WRITING"))
        .andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.endsWith("+09:00")));
  }

  @Test
  void updateSuccessor_onlyReviewer() throws Exception {
    UUID packageId = UUID.randomUUID();
    given(handoverPackageService.updateSuccessor(eq(packageId), any())).willReturn(response());

    mockMvc.perform(put(BASE_URL + "/" + packageId + "/successor")
            .contentType(MediaType.APPLICATION_JSON).content("{\"successorId\":null}")
            .header(HttpHeaders.AUTHORIZATION, bearer(Position.PREDECESSOR)))
        .andExpect(status().isForbidden());
    mockMvc.perform(put(BASE_URL + "/" + packageId + "/successor")
            .contentType(MediaType.APPLICATION_JSON).content("{\"successorId\":null}")
            .header(HttpHeaders.AUTHORIZATION, bearer(Position.REVIEWER)))
        .andExpect(status().isOk());
  }

  @Test
  void listAndGet_predecessorAndReviewerOnly() throws Exception {
    UUID packageId = UUID.randomUUID();
    given(handoverPackageService.list(any())).willReturn(List.of());
    given(handoverPackageService.get(any(), eq(packageId))).willReturn(response());

    for (Position position : List.of(Position.PREDECESSOR, Position.REVIEWER)) {
      mockMvc.perform(get(BASE_URL).header(HttpHeaders.AUTHORIZATION, bearer(position)))
          .andExpect(status().isOk());
      mockMvc.perform(get(BASE_URL + "/" + packageId).header(HttpHeaders.AUTHORIZATION, bearer(position)))
          .andExpect(status().isOk());
    }
    mockMvc.perform(get(BASE_URL).header(HttpHeaders.AUTHORIZATION, bearer(Position.SUCCESSOR)))
        .andExpect(status().isForbidden());
    mockMvc.perform(get(BASE_URL + "/" + packageId).header(HttpHeaders.AUTHORIZATION, bearer(Position.SUCCESSOR)))
        .andExpect(status().isForbidden());
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createRequest(
      Position position, String body) {
    return post(BASE_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .header(HttpHeaders.AUTHORIZATION, bearer(position));
  }

  private String validBody() {
    return "{\"predecessorId\":\"" + UUID.randomUUID() + "\",\"name\":\"인수인계\"}";
  }

  private HandoverPackageResponse response() {
    return new HandoverPackageResponse(UUID.randomUUID(), "인수인계", null, HandoverStatus.WRITING,
        new MemberResponse(UUID.randomUUID(), "predecessor", "전임자", Role.USER, Position.PREDECESSOR), null,
        OffsetDateTime.of(2026, 10, 17, 9, 0, 0, 0, ZoneOffset.ofHours(9)));
  }

  private String bearer(Position position) {
    return "Bearer " + jwtProvider.issue(new AuthenticatedMember(UUID.randomUUID(), "probe-user", Role.USER, position));
  }

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
