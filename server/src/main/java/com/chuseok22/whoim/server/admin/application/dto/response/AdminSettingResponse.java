package com.chuseok22.whoim.server.admin.application.dto.response;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.setting.application.dto.response.SettingConfigResponse;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import com.chuseok22.whoim.server.setting.core.constant.SettingValueType;
import java.time.OffsetDateTime;

public record AdminSettingResponse(
    String key,
    String description,
    SettingValueType valueType,
    boolean overridden,
    String effectiveValue,
    String defaultValue,
    OffsetDateTime updatedAt,
    double minimum,
    double maximum,
    int maxLength,
    String errorMessage
) {

  // 모델명처럼 기본값이 서버 설정(yml)인 키는 defaultValue가 null이고, effectiveValue만 빈 문자열로 바꾼다
  public static AdminSettingResponse from(SettingConfigResponse response) {
    SettingKey settingKey = SettingKey.fromKey(response.key())
        .orElseThrow(() -> new CustomException(ErrorCode.SETTING_KEY_NOT_FOUND));
    return new AdminSettingResponse(
        response.key(),
        response.description(),
        response.valueType(),
        response.overridden(),
        response.effectiveValue() == null ? "" : response.effectiveValue(),
        response.defaultValue(),
        response.updatedAt(),
        settingKey.minimum(),
        settingKey.maximum(),
        settingKey.maxLength(),
        null);
  }

  // 저장에 실패한 행은 사용자가 입력한 값을 그대로 다시 보여 주고 저장 상태는 바꾸지 않는다
  public AdminSettingResponse withInvalidInput(String inputValue, String message) {
    return new AdminSettingResponse(key, description, valueType, overridden, inputValue, defaultValue, updatedAt,
        minimum, maximum, maxLength, message);
  }
}
