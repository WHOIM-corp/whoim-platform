package com.chuseok22.whoim.server.auth.infrastructure.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

  @Test
  void shortKey_isRejected() {
    String shortKey = "x".repeat(63);

    assertThatThrownBy(() -> new JwtProperties(shortKey, 1000L, "issuer"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageNotContaining(shortKey);
  }

  @Test
  void nullOrBlankKey_isRejected() {
    assertThatThrownBy(() -> new JwtProperties(null, 1000L, "issuer")).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new JwtProperties("", 1000L, "issuer")).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new JwtProperties(" ".repeat(70), 1000L, "issuer")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void multiByteKeyUnder64Bytes_isMeasuredInBytesNotCharacters() {
    // 한글 한 글자는 3바이트라 21자(63바이트)는 거부, 22자(66바이트)는 통과한다
    assertThatThrownBy(() -> new JwtProperties("가".repeat(21), 1000L, "issuer")).isInstanceOf(IllegalStateException.class);
    assertThatCode(() -> new JwtProperties("가".repeat(22), 1000L, "issuer")).doesNotThrowAnyException();
  }

  @Test
  void keyOf64BytesOrMore_passes() {
    assertThat(new JwtProperties("y".repeat(64), 1000L, "issuer").secretKey()).hasSize(64);
    assertThatCode(() -> new JwtProperties("y".repeat(100), 1000L, "issuer")).doesNotThrowAnyException();
  }
}
