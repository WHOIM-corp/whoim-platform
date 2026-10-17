package com.chuseok22.whoim.server.setting.application.controller;

import com.chuseok22.whoim.server.setting.application.dto.request.SettingConfigUpdateRequest;
import com.chuseok22.whoim.server.setting.application.dto.response.SettingConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Setting", description = "서비스 설정 API (ADMIN 전용)")
public interface SettingConfigControllerDocs {

  @Operation(
      summary = "설정 목록 조회",
      description = """
          등록된 모든 설정을 레지스트리 순서로 반환한다. 오버라이드가 없으면 기본값이 effectiveValue다.
          모델명처럼 기본값이 서버 설정(yml)인 키는 defaultValue와 effectiveValue가 null이다.

          실패 케이스
          - 401 UNAUTHENTICATED: 토큰이 없거나 유효하지 않음
          - 403 ACCESS_DENIED: ADMIN이 아님
          """)
  ResponseEntity<List<SettingConfigResponse>> list();

  @Operation(
      summary = "설정 값 변경",
      description = """
          설정 값을 저장한다. 키에 점이 포함된다(예: llm.gemini.model). 값이 공백이면 오버라이드를 삭제해 기본값으로 되돌린다.

          실패 케이스
          - 400 INVALID_REQUEST: value가 null이거나 본문 형식 오류
          - 400 SETTING_VALUE_INVALID: 종류, 범위, 최대 길이 위반
          - 401 UNAUTHENTICATED: 토큰이 없거나 유효하지 않음
          - 403 ACCESS_DENIED: ADMIN이 아님
          - 404 SETTING_KEY_NOT_FOUND: 등록되지 않은 키
          """)
  ResponseEntity<SettingConfigResponse> update(String settingConfigKey, SettingConfigUpdateRequest request);
}
