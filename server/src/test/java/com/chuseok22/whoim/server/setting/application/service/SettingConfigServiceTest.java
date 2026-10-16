package com.chuseok22.whoim.server.setting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.chuseok22.whoim.server.setting.infrastructure.entity.SettingConfig;
import com.chuseok22.whoim.server.setting.infrastructure.repository.SettingConfigRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettingConfigServiceTest {

  private static final String MODEL_KEY = "llm.suh-ai.model";
  private static final String TEMPERATURE_KEY = "llm.suh-ai.temperature";

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

  private static SettingConfig config(String key, String value) {
    return SettingConfig.builder().configKey(key).configValue(value).build();
  }
}
