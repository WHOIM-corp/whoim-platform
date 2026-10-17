package com.chuseok22.whoim.server.admin.application.service;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminSettingGroupResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSettingResponse;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.setting.application.dto.response.SettingConfigResponse;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettingService {

  // 화면에 보이는 순서. 키 접두사로 그룹을 나눈다
  private static final List<Map.Entry<String, String>> GROUP_PREFIX_AND_NAME = List.of(
      Map.entry("llm.", "LLM"),
      Map.entry("gap.", "Gap 분석"),
      Map.entry("prompt.", "프롬프트 지침"),
      Map.entry("dlp.", "DLP 엔티티 사전"));

  private final SettingConfigService settingConfigService;

  public List<AdminSettingGroupResponse> groups() {
    List<AdminSettingResponse> settings = settingConfigService.list().stream()
        .map(AdminSettingResponse::from)
        .toList();
    return GROUP_PREFIX_AND_NAME.stream()
        .map(group -> new AdminSettingGroupResponse(group.getValue(), settings.stream()
            .filter(setting -> setting.key().startsWith(group.getKey()))
            .toList()))
        .toList();
  }

  // 값이 올바르지 않으면 저장하지 않고 오류 메시지를 담은 행을 돌려준다. 값은 로그에 남기지 않는다
  public AdminSettingResponse save(String settingConfigKey, String value) {
    try {
      return AdminSettingResponse.from(settingConfigService.update(settingConfigKey, value));
    } catch (CustomException exception) {
      if (exception.getErrorCode() != ErrorCode.SETTING_VALUE_INVALID) {
        throw exception;
      }
      log.warn("관리자 설정 저장이 검증에 실패했습니다: key={}", settingConfigKey);
      return currentSetting(settingConfigKey).withInvalidInput(value, ErrorCode.SETTING_VALUE_INVALID.getMessage());
    }
  }

  private AdminSettingResponse currentSetting(String settingConfigKey) {
    return settingConfigService.list().stream()
        .filter(response -> response.key().equals(settingConfigKey))
        .findFirst()
        .map(AdminSettingResponse::from)
        .orElseThrow(() -> new CustomException(ErrorCode.SETTING_KEY_NOT_FOUND));
  }
}
