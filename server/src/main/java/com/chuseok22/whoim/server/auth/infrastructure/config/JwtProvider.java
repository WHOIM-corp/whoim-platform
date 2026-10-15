package com.chuseok22.whoim.server.auth.infrastructure.config;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.auth.infrastructure.properties.JwtProperties;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

  private static final String USERNAME_CLAIM = "username";
  private static final String ROLE_CLAIM = "role";
  private static final String POSITION_CLAIM = "position";

  private final JwtProperties jwtProperties;
  private final Clock clock;
  private final SecretKey secretKey;

  public JwtProvider(JwtProperties jwtProperties, Clock clock) {
    this.jwtProperties = jwtProperties;
    this.clock = clock;
    this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8));
  }

  public String issue(AuthenticatedMember member) {
    Instant issuedAt = Instant.now(clock);
    var builder = Jwts.builder()
        .subject(member.memberId().toString())
        .claim(USERNAME_CLAIM, member.username())
        .claim(ROLE_CLAIM, member.role().name())
        .issuer(jwtProperties.issuer())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(issuedAt.plusMillis(jwtProperties.accessExpMillis())));
    if (member.position() != null) {
      builder.claim(POSITION_CLAIM, member.position().name());
    }
    return builder.signWith(secretKey, Jwts.SIG.HS512).compact();
  }

  // 만료는 EXPIRED_TOKEN, 그 외 모든 실패는 UNAUTHENTICATED CustomException으로 구분해 던진다
  public AuthenticatedMember parse(String token) {
    try {
      var jws = Jwts.parser()
          .verifyWith(secretKey)
          .requireIssuer(jwtProperties.issuer())
          .clock(() -> Date.from(Instant.now(clock)))
          .build()
          .parseSignedClaims(token);
      if (!Jwts.SIG.HS512.getId().equals(jws.getHeader().getAlgorithm())) {
        throw new CustomException(ErrorCode.UNAUTHENTICATED);
      }
      return toAuthenticatedMember(jws.getPayload());
    } catch (ExpiredJwtException e) {
      throw new CustomException(ErrorCode.EXPIRED_TOKEN, e);
    } catch (JwtException | IllegalArgumentException e) {
      throw new CustomException(ErrorCode.UNAUTHENTICATED, e);
    }
  }

  // 서명이 유효해도 필수 클레임이 없거나 enum에 없는 값이면 UNAUTHENTICATED로 처리한다
  private AuthenticatedMember toAuthenticatedMember(Claims claims) {
    try {
      String subject = requireClaim(claims.getSubject());
      String username = requireClaim(claims.get(USERNAME_CLAIM, String.class));
      Role role = Role.valueOf(requireClaim(claims.get(ROLE_CLAIM, String.class)));
      String position = claims.get(POSITION_CLAIM, String.class);
      return new AuthenticatedMember(
          UUID.fromString(subject),
          username,
          role,
          position == null ? null : Position.valueOf(position));
    } catch (IllegalArgumentException | ClassCastException e) {
      throw new CustomException(ErrorCode.UNAUTHENTICATED, e);
    }
  }

  private String requireClaim(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("필수 클레임이 비어 있습니다");
    }
    return value;
  }
}
