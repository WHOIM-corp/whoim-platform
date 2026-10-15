package com.chuseok22.whoim.server.auth.application.controller;

import com.chuseok22.whoim.server.auth.application.dto.request.LoginRequest;
import com.chuseok22.whoim.server.auth.application.dto.response.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerDocs {

  @SecurityRequirements
  @Operation(
      summary = "로그인",
      description = """
          아이디와 비밀번호로 로그인해 access token을 발급한다.

          실패 케이스
          - 400 INVALID_REQUEST: 아이디 또는 비밀번호가 비어 있음
          - 401 INVALID_CREDENTIALS: 아이디가 없거나 비밀번호가 일치하지 않음 (둘을 구분하지 않는다)
          """)
  ResponseEntity<LoginResponse> login(LoginRequest request);
}
