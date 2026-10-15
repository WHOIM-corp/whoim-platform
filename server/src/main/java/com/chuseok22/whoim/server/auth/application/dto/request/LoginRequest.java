package com.chuseok22.whoim.server.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Schema(description = "로그인 아이디", example = "predecessor")
    @NotBlank(message = "아이디를 입력해 주세요")
    String username,

    @Schema(description = "비밀번호", example = "password")
    @NotBlank(message = "비밀번호를 입력해 주세요")
    String password
) {

}
