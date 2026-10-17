package com.chuseok22.whoim.server.llm.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.chuseok22.whoim.server.llm.infrastructure.config.GeminiClientConfig;
import com.chuseok22.whoim.server.llm.infrastructure.properties.GeminiProperties;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
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

class GeminiProviderTest {

  private static final String BASE_URL = "https://gemini.test/v1beta";
  private static final String URL = BASE_URL + "/models/gemini-test-model:generateContent";
  private static final String SECRET_BODY = "SECRET-RESPONSE-BODY";

  private final GeminiProperties properties = new GeminiProperties(BASE_URL + "/", "test-key", "gemini-test-model");
  private final SettingConfigService settingConfigService = Mockito.mock(SettingConfigService.class);

  private MockRestServiceServer server;
  private GeminiProvider provider;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = GeminiClientConfig.configure(RestClient.builder(), properties);
    server = MockRestServiceServer.bindTo(builder).build();
    provider = new GeminiProvider(builder.build(), properties, settingConfigService);

    when(settingConfigService.getString(any(SettingKey.class), anyString()))
        .thenAnswer(invocation -> invocation.getArgument(1));
    when(settingConfigService.getDouble(any(SettingKey.class))).thenReturn(0.2);
  }

  @Test
  void generate_sendsExpectedRequest() {
    server.expect(requestTo(URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("x-goog-api-key", "test-key"))
        .andExpect(jsonPath("$.contents[0].parts[0].text").value("질문"))
        .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("시스템"))
        .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
        .andExpect(jsonPath("$.generationConfig.temperature").value(0.2))
        .andExpect(jsonPath("$.generationConfig.responseSchema").doesNotExist())
        .andExpect(jsonPath("$.generationConfig.responseJsonSchema").doesNotExist())
        .andRespond(withSuccess(textBody("{}"), MediaType.APPLICATION_JSON));

    provider.generate(new LlmRequest("시스템", "질문", Map.of("type", "object"), null));

    server.verify();
  }

  @Test
  void generate_usesModelFromSetting() {
    when(settingConfigService.getString(eq(SettingKey.LLM_GEMINI_MODEL), anyString())).thenReturn("gemini-other");
    server.expect(requestTo(BASE_URL + "/models/gemini-other:generateContent"))
        .andRespond(withSuccess(textBody("{}"), MediaType.APPLICATION_JSON));

    provider.generate(new LlmRequest(null, "질문", null, null));

    server.verify();
  }

  @Test
  void generate_usesTemperature_requestThenSettingThenDefault() {
    expectTemperature(0.7);
    provider.generate(new LlmRequest(null, "질문", null, 0.7));
    server.verify();

    server.reset();
    when(settingConfigService.getDouble(SettingKey.LLM_GEMINI_TEMPERATURE)).thenReturn(0.5);
    expectTemperature(0.5);
    provider.generate(new LlmRequest(null, "질문", null, null));
    server.verify();

    server.reset();
    when(settingConfigService.getDouble(SettingKey.LLM_GEMINI_TEMPERATURE)).thenReturn(0.2);
    expectTemperature(0.2);
    provider.generate(new LlmRequest(null, "질문", null, null));
    server.verify();
  }

  @Test
  void generate_omitsSystemInstruction_whenSystemNull() {
    server.expect(requestTo(URL))
        .andExpect(jsonPath("$.systemInstruction").doesNotExist())
        .andRespond(withSuccess(textBody("{}"), MediaType.APPLICATION_JSON));

    provider.generate(new LlmRequest(null, "질문", null, null));

    server.verify();
  }

  @Test
  void generate_joinsTextOfAllNonThoughtParts() {
    server.expect(requestTo(URL))
        .andRespond(withSuccess(
            "{\"candidates\":[{\"content\":{\"parts\":["
                + "{\"text\":\"생각\",\"thought\":true},"
                + "{\"text\":\"{\\\"a\\\":\"},"
                + "{\"text\":\"1}\"}]}}]}",
            MediaType.APPLICATION_JSON));

    LlmResponse response = provider.generate(new LlmRequest(null, "질문", null, null));

    assertThat(response).isEqualTo(new LlmResponse("{\"a\":1}", "GEMINI"));
  }

  @Test
  void generate_throwsLlmUnavailable_onHttpError() {
    server.expect(requestTo(URL))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body(SECRET_BODY));

    assertLlmUnavailableWithoutBody();
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{\"candidates\":[]}",
      "{}",
      "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}",
      "{\"candidates\":[{\"content\":{},\"finishReason\":\"SAFETY\"}]}",
      "{\"candidates\":[{\"content\":{\"parts\":[]}}]}",
      "{\"promptFeedback\":{\"blockReason\":\"SECRET-RESPONSE-BODY\"}}",
      "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"},\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}",
      "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"   \"}]}}]}",
      "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"생각\",\"thought\":true}]}}]}",
      "SECRET-RESPONSE-BODY not json"
  })
  void generate_throwsLlmUnavailable_onUnusablePayload(String body) {
    server.expect(requestTo(URL))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

    assertLlmUnavailableWithoutBody();
    server.verify();
  }

  private void expectTemperature(double temperature) {
    server.expect(requestTo(URL))
        .andExpect(jsonPath("$.generationConfig.temperature").value(temperature))
        .andRespond(withSuccess(textBody("{}"), MediaType.APPLICATION_JSON));
  }

  private String textBody(String text) {
    return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + text + "\"}]}}]}";
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
