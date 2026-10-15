package com.chuseok22.whoim.server.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.auth.infrastructure.properties.JwtProperties;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

  private static final String SECRET_KEY = "unit-test-dummy-secret-key-for-jwt-provider-0123456789-abcdefghijklmnopqrstuvwxyz";
  private static final String OTHER_SECRET_KEY = "another-dummy-secret-key-for-jwt-provider-9876543210-zyxwvutsrqponmlkjihgfedcba";
  private static final String ISSUER = "test-issuer";
  private static final long ACCESS_EXP_MILLIS = 3_600_000L;
  private static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");

  private final Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final JwtProvider jwtProvider = new JwtProvider(properties(SECRET_KEY), fixedClock);

  @Test
  void issueAndParse_roundTripsUserWithPosition() {
    AuthenticatedMember member = new AuthenticatedMember(UUID.randomUUID(), "predecessor-user", Role.USER, Position.PREDECESSOR);

    AuthenticatedMember parsed = jwtProvider.parse(jwtProvider.issue(member));

    assertThat(parsed).isEqualTo(member);
  }

  @Test
  void issueAndParse_adminHasNoPosition() {
    AuthenticatedMember member = new AuthenticatedMember(UUID.randomUUID(), "admin-user", Role.ADMIN, null);

    AuthenticatedMember parsed = jwtProvider.parse(jwtProvider.issue(member));

    assertThat(parsed.position()).isNull();
    assertThat(parsed.role()).isEqualTo(Role.ADMIN);
    assertThat(parsed.username()).isEqualTo("admin-user");
  }

  @Test
  void parse_expiredToken_throwsExpiredToken() {
    Clock past = Clock.fixed(NOW.minus(Duration.ofHours(2)), ZoneOffset.UTC);
    String token = new JwtProvider(properties(SECRET_KEY), past).issue(sampleMember());

    assertThatThrownBy(() -> jwtProvider.parse(token))
        .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EXPIRED_TOKEN));
  }

  @Test
  void parse_usesSameClockAsIssue_validBeforeAndExpiredAfterExpiry() {
    String token = jwtProvider.issue(sampleMember());
    Clock almostExpired = Clock.fixed(NOW.plusMillis(ACCESS_EXP_MILLIS - 1_000), ZoneOffset.UTC);
    Clock afterExpiry = Clock.fixed(NOW.plusMillis(ACCESS_EXP_MILLIS + 1_000), ZoneOffset.UTC);

    assertThat(new JwtProvider(properties(SECRET_KEY), almostExpired).parse(token).username()).isEqualTo("someone");
    assertThatThrownBy(() -> new JwtProvider(properties(SECRET_KEY), afterExpiry).parse(token))
        .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EXPIRED_TOKEN));
  }

  @Test
  void parse_tokenSignedWithDifferentKey_throwsUnauthenticated() {
    String token = new JwtProvider(properties(OTHER_SECRET_KEY), fixedClock).issue(sampleMember());

    assertUnauthenticated(token);
  }

  @Test
  void parse_tamperedToken_throwsUnauthenticated() {
    String token = jwtProvider.issue(sampleMember());
    String[] parts = token.split("\\.");
    String tamperedSignature = (parts[2].charAt(0) == 'A' ? "B" : "A") + parts[2].substring(1);

    assertUnauthenticated(parts[0] + "." + parts[1] + "." + tamperedSignature);
  }

  @Test
  void parse_malformedToken_throwsUnauthenticated() {
    assertUnauthenticated("not-a-jwt");
    assertUnauthenticated("");
  }

  @Test
  void parse_wrongIssuer_throwsUnauthenticated() {
    JwtProperties otherIssuer = new JwtProperties(SECRET_KEY, ACCESS_EXP_MILLIS, "other-issuer");
    String token = new JwtProvider(otherIssuer, fixedClock).issue(sampleMember());

    assertUnauthenticated(token);
  }

  @Test
  void parse_nonHs512Algorithm_throwsUnauthenticated() {
    String token = Jwts.builder()
        .subject(UUID.randomUUID().toString())
        .claim("username", "someone")
        .claim("role", "ADMIN")
        .issuer(ISSUER)
        .expiration(Date.from(NOW.plusSeconds(600)))
        .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
        .compact();

    assertUnauthenticated(token);
  }

  @Test
  void parse_tokenWithoutRoleClaim_throwsUnauthenticated() {
    assertUnauthenticated(tokenBuilder().subject(UUID.randomUUID().toString()).claim("username", "someone").compact());
  }

  @Test
  void parse_tokenWithoutSubject_throwsUnauthenticated() {
    assertUnauthenticated(tokenBuilder().claim("username", "someone").claim("role", "USER").compact());
  }

  @Test
  void parse_tokenWithUnknownRole_throwsUnauthenticated() {
    assertUnauthenticated(
        tokenBuilder().subject(UUID.randomUUID().toString()).claim("username", "someone").claim("role", "ROOT").compact());
  }

  private JwtBuilder tokenBuilder() {
    return Jwts.builder()
        .issuer(ISSUER)
        .expiration(Date.from(NOW.plusSeconds(600)))
        .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512);
  }

  private void assertUnauthenticated(String token) {
    assertThatThrownBy(() -> jwtProvider.parse(token))
        .isInstanceOfSatisfying(CustomException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
  }

  private AuthenticatedMember sampleMember() {
    return new AuthenticatedMember(UUID.randomUUID(), "someone", Role.USER, Position.REVIEWER);
  }

  private JwtProperties properties(String secretKey) {
    return new JwtProperties(secretKey, ACCESS_EXP_MILLIS, ISSUER);
  }
}
