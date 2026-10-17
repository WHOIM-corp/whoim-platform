package com.chuseok22.whoim.server.admin.application.service;

import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

  private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

  private final MemberRepository memberRepository;

  // 아이디 없음과 비ADMIN을 같은 예외로 돌려 로그인 실패 응답이 구분되지 않게 한다. ADMIN에는 POSITION 권한을 주지 않는다
  @Override
  public UserDetails loadUserByUsername(String username) {
    return memberRepository.findByUsername(username)
        .filter(member -> member.getRole() == Role.ADMIN)
        .map(member -> User.withUsername(member.getUsername())
            .password(member.getPassword())
            .authorities(new SimpleGrantedAuthority(ADMIN_AUTHORITY))
            .build())
        .orElseThrow(() -> new UsernameNotFoundException("관리자 계정이 아닙니다"));
  }
}
