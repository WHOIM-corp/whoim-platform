package com.chuseok22.whoim.server.auth.application.service;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.auth.application.dto.request.LoginRequest;
import com.chuseok22.whoim.server.auth.application.dto.response.LoginResponse;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtProvider;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.member.application.dto.response.MemberResponse;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;

  // 계정 존재 여부가 드러나지 않도록 아이디 없음과 비밀번호 불일치를 같은 오류로 응답한다
  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {
    Member member = memberRepository.findByUsername(request.username())
        .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));
    String accessToken = jwtProvider.issue(new AuthenticatedMember(
        member.getId(), member.getUsername(), member.getRole(), member.getPosition()));
    return new LoginResponse(accessToken, MemberResponse.from(member));
  }
}
