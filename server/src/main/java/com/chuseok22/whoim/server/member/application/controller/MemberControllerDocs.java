package com.chuseok22.whoim.server.member.application.controller;

import com.chuseok22.whoim.server.member.application.dto.response.MemberSearchResponse;
import com.chuseok22.whoim.server.member.core.constant.Position;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Member", description = "회원 조회 API")
public interface MemberControllerDocs {

  @Operation(
      summary = "position별 사원 키워드 검색",
      description = """
          승인자가 Package 생성 시 전임자·후임자를 고르기 위해 position별 사원을 검색한다.
          keyword는 이름·부서·직급·사번에 대소문자 구분 없이 부분 일치하며(%, _, \\ 는 글자 그대로),
          비우면 전체를 이름순으로 돌려준다. limit은 기본 6, 1~20으로 보정한다.

          실패 케이스
          - 400 INVALID_REQUEST: position 누락 또는 허용되지 않은 값
          - 403 ACCESS_DENIED: 승인자가 아님
          """)
  ResponseEntity<List<MemberSearchResponse>> list(
      @Parameter(description = "검색할 position") Position position,
      @Parameter(description = "검색어(이름·부서·직급·사번)") String keyword,
      @Parameter(description = "최대 결과 수(기본 6, 최대 20)") int limit);
}
