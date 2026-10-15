package com.chuseok22.whoim.server.common.application.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class SeoulTimeTest {

  @Test
  void toOffsetDateTime_utcInstant_returnsPlusNineOffset() {
    Instant instant = Instant.parse("2026-10-13T01:00:00Z");

    OffsetDateTime result = SeoulTime.toOffsetDateTime(instant);

    assertThat(result).hasToString("2026-10-13T10:00+09:00");
    assertThat(result.toInstant()).isEqualTo(instant);
  }

  @Test
  void toOffsetDateTime_null_returnsNull() {
    assertThat(SeoulTime.toOffsetDateTime(null)).isNull();
  }
}
