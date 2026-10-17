package com.chuseok22.whoim.server.setting.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record SettingConfigUpdateRequest(
    @NotNull(message = "설정 값은 필수입니다.")
    String value
) {

}
