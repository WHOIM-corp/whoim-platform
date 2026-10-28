package com.chuseok22.whoim.server.member.application.controller;

import com.chuseok22.logging.annotation.LogMonitoring;
import com.chuseok22.whoim.server.member.application.dto.response.MemberSearchResponse;
import com.chuseok22.whoim.server.member.application.service.MemberService;
import com.chuseok22.whoim.server.member.core.constant.Position;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController implements MemberControllerDocs {

  private final MemberService memberService;

  @LogMonitoring
  @Override
  @PreAuthorize("hasAuthority('POSITION_REVIEWER')")
  @GetMapping
  public ResponseEntity<List<MemberSearchResponse>> list(
      @RequestParam Position position,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "" + MemberService.DEFAULT_LIMIT) int limit) {
    return ResponseEntity.ok(memberService.search(position, keyword, limit));
  }
}
