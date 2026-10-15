package com.chuseok22.whoim.server.common.application.time;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class SeoulTime {

  public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private SeoulTime() {
  }

  // 저장은 Instant(UTC)이고 응답만 +09:00 오프셋으로 내려준다
  public static OffsetDateTime toOffsetDateTime(Instant instant) {
    if (instant == null) {
      return null;
    }
    return instant.atZone(ZONE).toOffsetDateTime();
  }
}
