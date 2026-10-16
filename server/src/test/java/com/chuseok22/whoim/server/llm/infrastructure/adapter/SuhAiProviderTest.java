package com.chuseok22.whoim.server.llm.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmResponse;
import com.chuseok22.whoim.server.llm.infrastructure.config.SuhAiClientConfig;
import com.chuseok22.whoim.server.llm.infrastructure.properties.SuhAiProperties;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SuhAiProviderTest {

  private static final String BASE_URL = "https://suh-ai.test/api";
  private static final String SECRET_BODY = "SECRET-RESPONSE-BODY";

  private final SuhAiProperties properties = new SuhAiProperties(BASE_URL + "/", "test-key", "gemma4:e4b");
  private final SettingConfigService settingConfigService = Mockito.mock(SettingConfigService.class);

  private MockRestServiceServer server;
  private SuhAiProvider provider;

  @BeforeEach
  void setUp() {
    // 운영 설정을 먼저 적용한 뒤 목 서버를 바인딩해야 목 요청 팩토리가 덮어써지지 않는다
    RestClient.Builder builder = SuhAiClientConfig.configure(RestClient.builder(), properties);
    server = MockRestServiceServer.bindTo(builder).build();
    provider = new SuhAiProvider(builder.build(), properties, settingConfigService);

    when(settingConfigService.getString(anyString(), anyString()))
        .thenAnswer(invocation -> invocation.getArgument(1));
    when(settingConfigService.getDouble(anyString(), anyDouble()))
        .thenAnswer(invocation -> invocation.getArgument(1));
  }

  @Test
  void generate_sendsExpectedRequest() {
    Map<String, Object> schema = Map.of("type", "object");
    server.expect(requestTo(BASE_URL + "/ollama/chat"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("X-API-Key", "test-key"))
        .andExpect(jsonPath("$.model").value("gemma4:e4b"))
        .andExpect(jsonPath("$.prompt").value("질문"))
        .andExpect(jsonPath("$.system").value("시스템"))
        .andExpect(jsonPath("$.temperature").value(0.2))
        .andExpect(jsonPath("$.auto_unload").value(false))
        .andExpect(jsonPath("$.format.type").value("object"))
        .andRespond(withSuccess(successBody("{}"), MediaType.APPLICATION_JSON));

    provider.generate(new LlmRequest("시스템", "질문", schema, null));

    server.verify();
  }

  @Test
  void generate_omitsSystemAndUsesJsonFormat_whenNull() {
    server.expect(requestTo(BASE_URL + "/ollama/chat"))
        .andExpect(jsonPath("$.system").doesNotExist())
        .andExpect(jsonPath("$.format").value("json"))
        .andRespond(withSuccess(successBody("{}"), MediaType.APPLICATION_JSON));

    provider.generate(new LlmRequest(null, "질문", null, null));

    server.verify();
  }

  @Test
  void generate_usesSettingModel_whenPresent() {
    when(settingConfigService.getString(eq("llm.suh-ai.model"), anyString())).thenReturn("gemma4:e2b");
    server.expect(requestTo(BASE_URL + "/ollama/chat"))
        .andExpect(jsonPath("$.model").value("gemma4:e2b"))
        .andRespond(withSuccess(successBody("{}"), MediaType.APPLICATION_JSON));

    provider.generate(new LlmRequest(null, "질문", null, null));

    server.verify();
  }

  @Test
  void generate_temperaturePrecedence() {
    // 요청 값 우선
    expectTemperature(0.7);
    provider.generate(new LlmRequest(null, "질문", null, 0.7));
    server.verify();

    // 요청 값이 없으면 setting 값
    server.reset();
    when(settingConfigService.getDouble(eq("llm.suh-ai.temperature"), anyDouble())).thenReturn(0.5);
    expectTemperature(0.5);
    provider.generate(new LlmRequest(null, "질문", null, null));
    server.verify();

    // 둘 다 없으면 기본값 0.2
    server.reset();
    when(settingConfigService.getDouble(eq("llm.suh-ai.temperature"), anyDouble()))
        .thenAnswer(invocation -> invocation.getArgument(1));
    expectTemperature(0.2);
    provider.generate(new LlmRequest(null, "질문", null, null));
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY})
  void generate_fallsBackToDefaultTemperature_whenSettingNotFinite(double invalid) {
    when(settingConfigService.getDouble(eq("llm.suh-ai.temperature"), anyDouble())).thenReturn(invalid);
    expectTemperature(0.2);

    provider.generate(new LlmRequest(null, "질문", null, null));

    server.verify();
  }

  @Test
  void generate_returnsContent_onSuccess() {
    server.expect(requestTo(BASE_URL + "/ollama/chat"))
        .andRespond(withSuccess(
            "{\"content\":\"{\\\"a\\\":1}\",\"metrics\":{\"total_duration_ms\":1200,\"prompt_eval_count\":3,"
                + "\"eval_count\":5},\"model\":\"gemma4:e4b\",\"success\":true}",
            MediaType.APPLICATION_JSON));

    LlmResponse response = provider.generate(new LlmRequest(null, "질문", null, null));

    assertThat(response).isEqualTo(new LlmResponse("{\"a\":1}", "SUH_AI"));
  }

  @Test
  void generate_succeeds_whenMetricsMissing() {
    server.expect(requestTo(BASE_URL + "/ollama/chat"))
        .andRespond(withSuccess("{\"content\":\"{}\",\"success\":true}", MediaType.APPLICATION_JSON));

    assertThat(provider.generate(new LlmRequest(null, "질문", null, null)).content()).isEqualTo("{}");
  }

  @Test
  void generate_failsWithLlmUnavailable_onHttpError500() {
    server.expect(requestTo(BASE_URL + "/ollama/chat"))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body(SECRET_BODY));

    assertLlmUnavailableWithoutBody();
    server.verify();
  }

  @Test
  void generate_failsWithLlmUnavailable_onHttpError401() {
    server.expect(requestTo(BASE_URL + "/ollama/chat"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body(SECRET_BODY));

    assertLlmUnavailableWithoutBody();
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{\"content\":\"{}\",\"success\":false}",
      "{\"content\":\"{}\",\"success\":true,\"error\":\"SECRET-RESPONSE-BODY\"}",
      "{\"content\":\"\",\"success\":true}",
      "{\"content\":\"  \",\"success\":true}",
      "{\"success\":true}",
      "{\"content\":\"{}\"}"
  })
  void generate_failsWithLlmUnavailable_onUnsuccessfulPayload(String body) {
    server.expect(requestTo(BASE_URL + "/ollama/chat"))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

    assertLlmUnavailableWithoutBody();
    server.verify();
  }

  @Test
  void generate_failsWithLlmUnavailable_onConnectionFailure() {
    server.expect(requestTo(BASE_URL + "/ollama/chat"))
        .andRespond(request -> {
          throw new IOException("connection refused");
        });

    assertLlmUnavailableWithoutBody();
    server.verify();
  }

  @Test
  void generate_failsWithLlmUnavailable_withoutLeakingMalformedBody() {
    server.expect(requestTo(BASE_URL + "/ollama/chat"))
        .andRespond(withSuccess("SECRET-token not json", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> provider.generate(new LlmRequest(null, "질문", null, null)))
        .isInstanceOfSatisfying(CustomException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);
          for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            assertThat(String.valueOf(cause.getMessage())).doesNotContain("SECRET-token");
          }
        });
  }

  private void expectTemperature(double temperature) {
    server.expect(requestTo(BASE_URL + "/ollama/chat"))
        .andExpect(jsonPath("$.temperature").value(temperature))
        .andRespond(withSuccess(successBody("{}"), MediaType.APPLICATION_JSON));
  }

  private String successBody(String content) {
    return "{\"content\":\"" + content + "\",\"metrics\":{},\"model\":\"gemma4:e4b\",\"success\":true}";
  }

  private void assertLlmUnavailableWithoutBody() {
    assertThatThrownBy(() -> provider.generate(new LlmRequest(null, "질문", null, null)))
        .isInstanceOfSatisfying(CustomException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);
          for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            assertThat(String.valueOf(cause.getMessage())).doesNotContain(SECRET_BODY);
          }
        });
  }
}
