package com.chuseok22.whoim.server.setting.application.service;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.setting.application.dto.response.SettingConfigResponse;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import com.chuseok22.whoim.server.setting.infrastructure.entity.SettingConfig;
import com.chuseok22.whoim.server.setting.infrastructure.repository.SettingConfigRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

  @Transactional(readOnly = true)
  public String getString(SettingKey key, String fallback) {
    return getString(key.key(), fallback);
  }

  @Transactional(readOnly = true)
  public int getInt(SettingKey key) {
    return getInt(key.key(), Integer.parseInt(key.defaultValue()));
  }

  @Transactional(readOnly = true)
  public double getDouble(SettingKey key) {
    double defaultValue = Double.parseDouble(key.defaultValue());
    double value = getDouble(key.key(), defaultValue);
    // NaN/Infinity는 JSON 직렬화가 불가능하거나 서버가 거부하므로 기본값으로 대체한다
    return Double.isFinite(value) ? value : defaultValue;
  }

  @Transactional(readOnly = true)
  public List<SettingConfigResponse> list() {
    return Arrays.stream(SettingKey.values())
        .map(settingKey -> SettingConfigResponse.from(settingKey, settingConfigRepository.findByConfigKey(settingKey.key())))
        .toList();
  }

  @Transactional
  public SettingConfigResponse update(String configKey, String value) {
    SettingKey settingKey = SettingKey.fromKey(configKey)
        .orElseThrow(() -> new CustomException(ErrorCode.SETTING_KEY_NOT_FOUND));
    Optional<SettingConfig> existing = settingConfigRepository.findByConfigKey(settingKey.key());

    // 공백은 오버라이드 삭제(기본값 복귀)다. 삭제할 행이 없어도 성공한다
    if (value == null || value.isBlank()) {
      existing.ifPresent(settingConfigRepository::delete);
      log.info("setting_config 오버라이드를 삭제했습니다: key={}", settingKey.key());
      return SettingConfigResponse.from(settingKey, Optional.empty());
    }

    settingKey.validate(value);
    SettingConfig saved = existing
        .map(config -> {
          config.changeValue(value);
          return config;
        })
        .orElseGet(() -> SettingConfig.builder().configKey(settingKey.key()).configValue(value).build());
    // updated_at이 응답에 반영되도록 즉시 flush한다
    saved = settingConfigRepository.saveAndFlush(saved);
    log.info("setting_config 값을 저장했습니다: key={}", settingKey.key());
    return SettingConfigResponse.from(settingKey, Optional.of(saved));
  }
}
