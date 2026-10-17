package com.chuseok22.whoim.server.member.application.controller;

import com.chuseok22.whoim.server.member.application.dto.response.MemberResponse;
import com.chuseok22.whoim.server.member.core.constant.Position;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Member", description = "회원 조회 API")
public interface MemberControllerDocs {

  @Operation(
      summary = "position별 회원 목록 조회",
      description = """
          승인자가 Package 생성 시 전임자·후임자를 고르기 위해 position별 회원을 이름순으로 조회한다.

          실패 케이스
          - 400 INVALID_REQUEST: position 누락 또는 허용되지 않은 값
          - 403 ACCESS_DENIED: 승인자가 아님
          """)
  ResponseEntity<List<MemberResponse>> list(Position position);
}
