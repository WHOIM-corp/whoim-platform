package com.chuseok22.whoim.server.setting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.setting.application.dto.response.SettingConfigResponse;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import com.chuseok22.whoim.server.setting.infrastructure.entity.SettingConfig;
import com.chuseok22.whoim.server.setting.infrastructure.repository.SettingConfigRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettingConfigServiceTest {

  private static final String MODEL_KEY = "llm.suh-ai.model";
  private static final String TEMPERATURE_KEY = "llm.suh-ai.temperature";

  private static final String MAX_INPUT_KEY = "gap.analysis.max-input-characters";

  @Mock
  private SettingConfigRepository settingConfigRepository;

  @InjectMocks
  private SettingConfigService settingConfigService;

  @Test
  void getString_returnsStoredValue() {
    when(settingConfigRepository.findByConfigKey(MODEL_KEY)).thenReturn(Optional.of(config(MODEL_KEY, "gemma4:e2b")));

    assertThat(settingConfigService.getString(MODEL_KEY, "gemma4:e4b")).isEqualTo("gemma4:e2b");
  }

  @Test
  void getString_returnsDefault_whenRowAbsent() {
    when(settingConfigRepository.findByConfigKey(MODEL_KEY)).thenReturn(Optional.empty());

    assertThat(settingConfigService.getString(MODEL_KEY, "gemma4:e4b")).isEqualTo("gemma4:e4b");
  }

  @Test
  void getString_returnsDefault_whenValueBlank() {
    when(settingConfigRepository.findByConfigKey(MODEL_KEY)).thenReturn(Optional.of(config(MODEL_KEY, "  ")));

    assertThat(settingConfigService.getString(MODEL_KEY, "gemma4:e4b")).isEqualTo("gemma4:e4b");
  }

  @Test
  void getString_returnsTrimmedValue() {
    when(settingConfigRepository.findByConfigKey(MODEL_KEY)).thenReturn(Optional.of(config(MODEL_KEY, " gemma4:e2b ")));

    assertThat(settingConfigService.getString(MODEL_KEY, "gemma4:e4b")).isEqualTo("gemma4:e2b");
  }

  @Test
  void getDouble_parsesStoredValue() {
    when(settingConfigRepository.findByConfigKey(TEMPERATURE_KEY)).thenReturn(Optional.of(config(TEMPERATURE_KEY, "0.7")));

    assertThat(settingConfigService.getDouble(TEMPERATURE_KEY, 0.2)).isEqualTo(0.7);
  }

  @Test
  void getDouble_returnsDefault_whenNotNumeric() {
    when(settingConfigRepository.findByConfigKey(TEMPERATURE_KEY)).thenReturn(Optional.of(config(TEMPERATURE_KEY, "abc")));

    assertThat(settingConfigService.getDouble(TEMPERATURE_KEY, 0.2)).isEqualTo(0.2);
  }

  @Test
  void getInt_returnsStoredValue() {
    when(settingConfigRepository.findByConfigKey(MAX_INPUT_KEY)).thenReturn(Optional.of(config(MAX_INPUT_KEY, "30000")));

    assertThat(settingConfigService.getInt(MAX_INPUT_KEY, 20000)).isEqualTo(30000);
  }

  @Test
  void getInt_returnsDefaultWhenMissing() {
    when(settingConfigRepository.findByConfigKey(MAX_INPUT_KEY)).thenReturn(Optional.empty());

    assertThat(settingConfigService.getInt(MAX_INPUT_KEY, 20000)).isEqualTo(20000);
  }

  @Test
  void getInt_returnsDefaultWhenNotNumber() {
    when(settingConfigRepository.findByConfigKey(MAX_INPUT_KEY)).thenReturn(Optional.of(config(MAX_INPUT_KEY, "abc")));

    assertThat(settingConfigService.getInt(MAX_INPUT_KEY, 20000)).isEqualTo(20000);
  }

  @Test
  void getInt_returnsDefaultWhenBlank() {
    when(settingConfigRepository.findByConfigKey(MAX_INPUT_KEY)).thenReturn(Optional.of(config(MAX_INPUT_KEY, "  ")));

    assertThat(settingConfigService.getInt(MAX_INPUT_KEY, 20000)).isEqualTo(20000);
  }

  @Test
  void getDouble_usesRegistryDefault_whenRowAbsent() {
    when(settingConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.empty());

    assertThat(settingConfigService.getDouble(SettingKey.LLM_SUH_AI_TEMPERATURE)).isEqualTo(0.0);
    assertThat(settingConfigService.getDouble(SettingKey.LLM_GEMINI_TEMPERATURE)).isEqualTo(0.2);
  }

  @Test
  void getDouble_usesRegistryDefault_whenStoredValueNotFinite() {
    when(settingConfigRepository.findByConfigKey(TEMPERATURE_KEY)).thenReturn(Optional.of(config(TEMPERATURE_KEY, "NaN")));

    assertThat(settingConfigService.getDouble(SettingKey.LLM_SUH_AI_TEMPERATURE)).isEqualTo(0.0);
  }

  @Test
  void getInt_usesRegistryDefault() {
    when(settingConfigRepository.findByConfigKey(MAX_INPUT_KEY)).thenReturn(Optional.empty());

    assertThat(settingConfigService.getInt(SettingKey.GAP_ANALYSIS_MAX_INPUT_CHARACTERS)).isEqualTo(20000);
  }

  @Test
  void getString_returnsFallback_whenRowAbsent() {
    when(settingConfigRepository.findByConfigKey(MODEL_KEY)).thenReturn(Optional.empty());

    assertThat(settingConfigService.getString(SettingKey.LLM_SUH_AI_MODEL, "yml-model")).isEqualTo("yml-model");
  }

  @Test
  void getLines_trimsDropsBlanksAndDuplicatesKeepingOrder() {
    String key = SettingKey.DLP_ENTITIES_CUSTOMER.key();
    when(settingConfigRepository.findByConfigKey(key))
        .thenReturn(Optional.of(config(key, " 가나다상사 \n\n라마바물류\r\n가나다상사\n  \n마바사")));

    assertThat(settingConfigService.getLines(SettingKey.DLP_ENTITIES_CUSTOMER))
        .containsExactly("가나다상사", "라마바물류", "마바사");
  }

  @Test
  void getLines_returnsEmpty_whenNoRow() {
    when(settingConfigRepository.findByConfigKey(SettingKey.DLP_ENTITIES_PERSON.key())).thenReturn(Optional.empty());

    assertThat(settingConfigService.getLines(SettingKey.DLP_ENTITIES_PERSON)).isEmpty();
  }

  @Test
  void list_showsEmptyEffectiveValueForDictionaryKeys() {
    when(settingConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.empty());

    SettingConfigResponse response = find(settingConfigService.list(), "dlp.entities.customer");

    assertThat(response.defaultValue()).isEmpty();
    assertThat(response.effectiveValue()).isEmpty();
    assertThat(response.overridden()).isFalse();
  }

  @Test
  void list_returnsEveryRegisteredKeyInOrder() {
    when(settingConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.empty());

    List<SettingConfigResponse> responses = settingConfigService.list();

    assertThat(responses).extracting(SettingConfigResponse::key)
        .containsExactlyElementsOf(java.util.Arrays.stream(SettingKey.values()).map(SettingKey::key).toList());
  }

  @Test
  void list_marksOverriddenAndEffectiveValue() {
    when(settingConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.empty());
    when(settingConfigRepository.findByConfigKey(TEMPERATURE_KEY)).thenReturn(Optional.of(config(TEMPERATURE_KEY, "0.7")));

    SettingConfigResponse response = find(settingConfigService.list(), TEMPERATURE_KEY);

    assertThat(response.overridden()).isTrue();
    assertThat(response.defaultValue()).isEqualTo("0.0");
    assertThat(response.effectiveValue()).isEqualTo("0.7");
  }

  @Test
  void list_showsDefaultWhenRowAbsent() {
    when(settingConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.empty());

    List<SettingConfigResponse> responses = settingConfigService.list();

    SettingConfigResponse temperature = find(responses, TEMPERATURE_KEY);
    assertThat(temperature.overridden()).isFalse();
    assertThat(temperature.effectiveValue()).isEqualTo("0.0");
    SettingConfigResponse model = find(responses, MODEL_KEY);
    assertThat(model.defaultValue()).isNull();
    assertThat(model.effectiveValue()).isNull();
  }

  @Test
  void update_createsRow_whenAbsent() {
    when(settingConfigRepository.findByConfigKey(TEMPERATURE_KEY)).thenReturn(Optional.empty());
    when(settingConfigRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(SettingConfig.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    SettingConfigResponse response = settingConfigService.update(TEMPERATURE_KEY, "0.5");

    ArgumentCaptor<SettingConfig> captor = ArgumentCaptor.forClass(SettingConfig.class);
    verify(settingConfigRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getConfigKey()).isEqualTo(TEMPERATURE_KEY);
    assertThat(captor.getValue().getConfigValue()).isEqualTo("0.5");
    assertThat(response.overridden()).isTrue();
    assertThat(response.effectiveValue()).isEqualTo("0.5");
  }

  @Test
  void update_changesValue_whenPresent() {
    SettingConfig existing = config(TEMPERATURE_KEY, "0.7");
    when(settingConfigRepository.findByConfigKey(TEMPERATURE_KEY)).thenReturn(Optional.of(existing));
    when(settingConfigRepository.saveAndFlush(existing)).thenReturn(existing);

    SettingConfigResponse response = settingConfigService.update(TEMPERATURE_KEY, "1.2");

    assertThat(existing.getConfigValue()).isEqualTo("1.2");
    assertThat(response.effectiveValue()).isEqualTo("1.2");
  }

  @Test
  void update_throwsKeyNotFound_forUnregisteredKey() {
    assertThatThrownBy(() -> settingConfigService.update("unknown.key", "1"))
        .isInstanceOfSatisfying(CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SETTING_KEY_NOT_FOUND));
  }

  @Test
  void update_throwsValueInvalid_forEachViolation() {
    assertInvalid("prompt.gap-analysis", "가".repeat(8001));
    assertInvalid(TEMPERATURE_KEY, "2.5");
    assertInvalid(TEMPERATURE_KEY, "abc");
    assertInvalid(MAX_INPUT_KEY, "1.5");
    assertInvalid(MAX_INPUT_KEY, "0");
    verify(settingConfigRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void update_deletesOverride_whenValueBlank() {
    SettingConfig existing = config(TEMPERATURE_KEY, "0.7");
    when(settingConfigRepository.findByConfigKey(TEMPERATURE_KEY)).thenReturn(Optional.of(existing));

    SettingConfigResponse response = settingConfigService.update(TEMPERATURE_KEY, "  ");

    verify(settingConfigRepository).delete(existing);
    assertThat(response.overridden()).isFalse();
    assertThat(response.effectiveValue()).isEqualTo("0.0");
  }

  @Test
  void update_blankIsNoop_whenNoRow() {
    when(settingConfigRepository.findByConfigKey(TEMPERATURE_KEY)).thenReturn(Optional.empty());

    SettingConfigResponse response = settingConfigService.update(TEMPERATURE_KEY, "");

    verify(settingConfigRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    assertThat(response.overridden()).isFalse();
  }

  @Test
  void update_doesNotLogValue() {
    Logger logger = (Logger) LoggerFactory.getLogger(SettingConfigService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      when(settingConfigRepository.findByConfigKey(TEMPERATURE_KEY)).thenReturn(Optional.empty());
      when(settingConfigRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(SettingConfig.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      settingConfigService.update(TEMPERATURE_KEY, "1.2345");

      assertThat(appender.list).isNotEmpty();
      assertThat(appender.list).noneMatch(event -> event.getFormattedMessage().contains("1.2345"));
    } finally {
      logger.detachAppender(appender);
    }
  }

  private void assertInvalid(String key, String value) {
    assertThatThrownBy(() -> settingConfigService.update(key, value))
        .isInstanceOfSatisfying(CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SETTING_VALUE_INVALID));
  }

  private static SettingConfigResponse find(List<SettingConfigResponse> responses, String key) {
    return responses.stream().filter(response -> response.key().equals(key)).findFirst().orElseThrow();
  }

  private static SettingConfig config(String key, String value) {
    return SettingConfig.builder().configKey(key).configValue(value).build();
  }
}
