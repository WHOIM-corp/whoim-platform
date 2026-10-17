package com.chuseok22.whoim.server.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminSettingGroupResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSettingResponse;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.setting.application.dto.response.SettingConfigResponse;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminSettingServiceTest {

  @Mock
  private SettingConfigService settingConfigService;

  @InjectMocks
  private AdminSettingService adminSettingService;

  @Test
  void groups_returnsFourGroupsInOrder() {
    given(settingConfigService.list()).willReturn(defaultResponses());

    List<AdminSettingGroupResponse> groups = adminSettingService.groups();

    assertThat(groups).extracting(AdminSettingGroupResponse::name)
        .containsExactly("LLM", "Gap 분석", "프롬프트 지침", "DLP 엔티티 사전");
    assertThat(groups).extracting(group -> group.settings().size()).containsExactly(4, 1, 5, 4);
  }

  @Test
  void groups_exposesHintsAndKeepsNullDefaultAsNull() {
    given(settingConfigService.list()).willReturn(defaultResponses());

    AdminSettingResponse model = adminSettingService.groups().get(0).settings().stream()
        .filter(setting -> setting.key().equals("llm.gemini.model"))
        .findFirst().orElseThrow();

    assertThat(model.defaultValue()).isNull();
    assertThat(model.effectiveValue()).isEmpty();
    assertThat(model.maxLength()).isEqualTo(200);
    assertThat(model.errorMessage()).isNull();
  }

  @Test
  void save_returnsErrorMessageAndInputValue_whenValueInvalid() {
    given(settingConfigService.update("gap.analysis.max-input-characters", "2000000"))
        .willThrow(new CustomException(ErrorCode.SETTING_VALUE_INVALID));
    given(settingConfigService.list()).willReturn(defaultResponses());

    AdminSettingResponse result = adminSettingService.save("gap.analysis.max-input-characters", "2000000");

    assertThat(result.errorMessage()).isEqualTo(ErrorCode.SETTING_VALUE_INVALID.getMessage());
    assertThat(result.effectiveValue()).isEqualTo("2000000");
    assertThat(result.overridden()).isFalse();
  }

  @Test
  void save_blankValueDeletesOverride() {
    given(settingConfigService.update("llm.gemini.temperature", ""))
        .willReturn(SettingConfigResponse.from(SettingKey.LLM_GEMINI_TEMPERATURE, Optional.empty()));

    AdminSettingResponse result = adminSettingService.save("llm.gemini.temperature", "");

    verify(settingConfigService).update("llm.gemini.temperature", "");
    assertThat(result.overridden()).isFalse();
    assertThat(result.effectiveValue()).isEqualTo("0.2");
    assertThat(result.errorMessage()).isNull();
  }

  @Test
  void save_propagatesKeyNotFound() {
    given(settingConfigService.update("unknown.key", "1"))
        .willThrow(new CustomException(ErrorCode.SETTING_KEY_NOT_FOUND));

    assertThatThrownBy(() -> adminSettingService.save("unknown.key", "1"))
        .isInstanceOfSatisfying(CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SETTING_KEY_NOT_FOUND));
  }

  private static List<SettingConfigResponse> defaultResponses() {
    return Arrays.stream(SettingKey.values())
        .map(key -> SettingConfigResponse.from(key, Optional.empty()))
        .toList();
  }
}
