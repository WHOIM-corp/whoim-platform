package com.chuseok22.whoim.server.setting.application.dto.response;

import com.chuseok22.whoim.server.common.application.time.SeoulTime;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import com.chuseok22.whoim.server.setting.core.constant.SettingValueType;
import com.chuseok22.whoim.server.setting.infrastructure.entity.SettingConfig;
import java.time.OffsetDateTime;
import java.util.Optional;

public record SettingConfigResponse(
    String key,
    String description,
    SettingValueType valueType,
    String defaultValue,
    String effectiveValue,
    boolean overridden,
    OffsetDateTime updatedAt
) {

  public static SettingConfigResponse from(SettingKey settingKey, Optional<SettingConfig> settingConfig) {
    return new SettingConfigResponse(
        settingKey.key(),
        settingKey.description(),
        settingKey.valueType(),
        settingKey.defaultValue(),
        settingConfig.map(SettingConfig::getConfigValue).orElse(settingKey.defaultValue()),
        settingConfig.isPresent(),
        settingConfig.map(SettingConfig::getUpdatedAt).map(SeoulTime::toOffsetDateTime).orElse(null));
  }
}
