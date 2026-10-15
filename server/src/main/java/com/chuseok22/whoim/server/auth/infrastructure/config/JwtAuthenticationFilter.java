package com.chuseok22.whoim.server.auth.infrastructure.config;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// 클레임만 신뢰하고 DB를 조회하지 않는다. 실패해도 컨텍스트를 비워 둔 채 체인을 계속하고, 401은 entry point가 응답한다
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String ERROR_CODE_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".ERROR_CODE";

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtProvider jwtProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
      authenticate(request, authorization.substring(BEARER_PREFIX.length()).trim());
    }
    filterChain.doFilter(request, response);
  }

  private void authenticate(HttpServletRequest request, String token) {
    try {
      AuthenticatedMember member = jwtProvider.parse(token);
      var authentication = new UsernamePasswordAuthenticationToken(member, null, toAuthorities(member));
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (CustomException e) {
      SecurityContextHolder.clearContext();
      request.setAttribute(ERROR_CODE_ATTRIBUTE, e.getErrorCode());
    }
  }

  private List<GrantedAuthority> toAuthorities(AuthenticatedMember member) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_" + member.role().name()));
    if (member.position() != null) {
      authorities.add(new SimpleGrantedAuthority("POSITION_" + member.position().name()));
    }
    return authorities;
  }
}
