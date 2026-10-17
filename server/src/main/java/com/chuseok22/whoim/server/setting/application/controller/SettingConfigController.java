package com.chuseok22.whoim.server.setting.application.controller;

import com.chuseok22.whoim.server.setting.application.dto.request.SettingConfigUpdateRequest;
import com.chuseok22.whoim.server.setting.application.dto.response.SettingConfigResponse;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 설정 값(프롬프트 지침 등)이 로그에 남을 수 있어 @LogMonitoring을 붙이지 않는다
@RestController
@RequestMapping("/api/setting-configs")
@RequiredArgsConstructor
public class SettingConfigController implements SettingConfigControllerDocs {

  private final SettingConfigService settingConfigService;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public ResponseEntity<List<SettingConfigResponse>> list() {
    return ResponseEntity.ok(settingConfigService.list());
  }

  // 키에 점이 있어 :.+ 로 확장자 해석을 막는다
  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{setting-config-key:.+}")
  public ResponseEntity<SettingConfigResponse> update(
      @PathVariable("setting-config-key") String settingConfigKey,
      @Valid @RequestBody SettingConfigUpdateRequest request) {
    return ResponseEntity.ok(settingConfigService.update(settingConfigKey, request.value()));
  }
}
