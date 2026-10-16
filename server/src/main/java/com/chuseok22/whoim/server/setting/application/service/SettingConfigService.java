package com.chuseok22.whoim.server.setting.application.service;

import com.chuseok22.whoim.server.setting.infrastructure.entity.SettingConfig;
import com.chuseok22.whoim.server.setting.infrastructure.repository.SettingConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingConfigService {

  private final SettingConfigRepository settingConfigRepository;

  @Transactional(readOnly = true)
  public String getString(String configKey, String defaultValue) {
    return settingConfigRepository.findByConfigKey(configKey)
        .map(SettingConfig::getConfigValue)
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .orElse(defaultValue);
  }

  @Transactional(readOnly = true)
  public int getInt(String configKey, int defaultValue) {
    String value = getString(configKey, null);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      // 값은 남기지 않고 키 이름만 남긴다
      log.warn("setting_config 값이 정수가 아니어서 기본값을 사용합니다: key={}", configKey);
      return defaultValue;
    }
  }

  @Transactional(readOnly = true)
  public double getDouble(String configKey, double defaultValue) {
    String value = getString(configKey, null);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException exception) {
      // 값은 남기지 않고 키 이름만 남긴다
      log.warn("setting_config 값이 숫자가 아니어서 기본값을 사용합니다: key={}", configKey);
      return defaultValue;
    }
  }
}
