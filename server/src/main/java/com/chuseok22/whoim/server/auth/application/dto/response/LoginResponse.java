package com.chuseok22.whoim.server.auth.application.dto.response;

import com.chuseok22.whoim.server.member.application.dto.response.MemberResponse;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
    @Schema(description = "JWT access token. Authorization: Bearer 헤더에 담아 보낸다")
    String accessToken,

    @Schema(description = "로그인한 계정 정보")
    MemberResponse member
) {

}
