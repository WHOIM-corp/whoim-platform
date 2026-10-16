package com.chuseok22.whoim.server.interview.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TraceStateCodecTest {

  private final TraceStateCodec codec = new TraceStateCodec(JsonMapper.builder().build());

  @Test
  void writeThenRead_roundTrips() {
    TraceState state = TraceState.empty().merge(Map.of(
        TraceElement.CAUSE, new TraceStateEntry(TraceElementStatus.FILLED, "원인", EvidenceLinkType.RECORDED, 2),
        TraceElement.RISK, new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, 3)));

    TraceState read = codec.read(codec.write(state));

    assertThat(read).isEqualTo(state);
  }

  @Test
  void read_emptyObjectIsEmptyState() {
    assertThat(codec.read("{}").entries()).isEmpty();
  }

  @Test
  void read_jsonNullThrowsInternalServerError() {
    assertThatThrownBy(() -> codec.read("null"))
        .isInstanceOfSatisfying(CustomException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
  }

  @Test
  void read_brokenJsonThrowsInternalServerError() {
    assertThatThrownBy(() -> codec.read("{broken"))
        .isInstanceOfSatisfying(CustomException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
  }
}
