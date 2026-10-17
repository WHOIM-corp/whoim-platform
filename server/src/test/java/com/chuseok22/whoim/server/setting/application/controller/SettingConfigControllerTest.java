package com.chuseok22.whoim.server.setting.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.chuseok22.whoim.server.setting.application.dto.response.SettingConfigResponse;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import com.chuseok22.whoim.server.setting.core.constant.SettingValueType;
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

@WebMvcTest(controllers = SettingConfigController.class)
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
    SettingConfigControllerTest.FixedClockConfig.class
})
class SettingConfigControllerTest {

  private static final String BASE_URL = "/api/setting-configs";
  private static final String GEMINI_MODEL_KEY = "llm.gemini.model";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @MockitoBean
  private SettingConfigService settingConfigService;

  @Test
  void list_returns200_forAdmin() throws Exception {
    given(settingConfigService.list()).willReturn(List.of(response("llm.suh-ai.temperature", "0.0", "0.5", true)));

    mockMvc.perform(get(BASE_URL).header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN, null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].key").value("llm.suh-ai.temperature"))
        .andExpect(jsonPath("$[0].overridden").value(true))
        .andExpect(jsonPath("$[0].effectiveValue").value("0.5"))
        .andExpect(jsonPath("$[0].updatedAt").value(org.hamcrest.Matchers.endsWith("+09:00")));
  }

  @Test
  void put_returns200_forAdmin() throws Exception {
    given(settingConfigService.update("llm.suh-ai.temperature", "0.5"))
        .willReturn(response("llm.suh-ai.temperature", "0.0", "0.5", true));

    mockMvc.perform(put(BASE_URL + "/llm.suh-ai.temperature")
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN, null))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"value\":\"0.5\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.effectiveValue").value("0.5"));
  }

  @Test
  void put_works_forDottedKey() throws Exception {
    given(settingConfigService.update(eq(GEMINI_MODEL_KEY), eq("gemini-test")))
        .willReturn(response(GEMINI_MODEL_KEY, null, "gemini-test", true));

    mockMvc.perform(put(BASE_URL + "/" + GEMINI_MODEL_KEY)
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN, null))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"value\":\"gemini-test\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.key").value(GEMINI_MODEL_KEY));
  }

  @Test
  void noToken_returns401() throws Exception {
    mockMvc.perform(get(BASE_URL))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    mockMvc.perform(put(BASE_URL + "/" + GEMINI_MODEL_KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"value\":\"x\"}"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(settingConfigService);
  }

  @Test
  void userWithAnyPosition_returns403() throws Exception {
    for (Position position : Position.values()) {
      mockMvc.perform(get(BASE_URL).header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, position)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
      mockMvc.perform(put(BASE_URL + "/" + GEMINI_MODEL_KEY)
              .header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, position))
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"value\":\"x\"}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    verifyNoInteractions(settingConfigService);
  }

  @Test
  void put_unregisteredKey_returns404() throws Exception {
    given(settingConfigService.update(eq("unknown.key"), any()))
        .willThrow(new CustomException(ErrorCode.SETTING_KEY_NOT_FOUND));

    mockMvc.perform(put(BASE_URL + "/unknown.key")
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN, null))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"value\":\"x\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("SETTING_KEY_NOT_FOUND"));
  }

  @Test
  void put_invalidValue_returns400() throws Exception {
    given(settingConfigService.update(eq("llm.suh-ai.temperature"), any()))
        .willThrow(new CustomException(ErrorCode.SETTING_VALUE_INVALID));

    mockMvc.perform(put(BASE_URL + "/llm.suh-ai.temperature")
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN, null))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"value\":\"9\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("SETTING_VALUE_INVALID"));
  }

  @Test
  void put_nullValue_returns400InvalidRequest() throws Exception {
    mockMvc.perform(put(BASE_URL + "/" + GEMINI_MODEL_KEY)
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN, null))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"value\":null}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    mockMvc.perform(put(BASE_URL + "/" + GEMINI_MODEL_KEY)
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN, null))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

    verifyNoInteractions(settingConfigService);
  }

  private static SettingConfigResponse response(String key, String defaultValue, String effectiveValue,
      boolean overridden) {
    return new SettingConfigResponse(key, "설명", SettingValueType.STRING, defaultValue, effectiveValue, overridden,
        OffsetDateTime.of(2026, 10, 17, 9, 0, 0, 0, ZoneOffset.ofHours(9)));
  }

  private String bearer(Role role, Position position) {
    return "Bearer " + jwtProvider.issue(new AuthenticatedMember(UUID.randomUUID(), "probe-user", role, position));
  }

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"), ZoneOffset.UTC);
    }
  }
}
