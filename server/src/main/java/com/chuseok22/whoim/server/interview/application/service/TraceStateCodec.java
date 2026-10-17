package com.chuseok22.whoim.server.interview.application.service;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraceStateCodec {

  private static final TypeReference<Map<TraceElement, TraceStateEntry>> ENTRIES_TYPE = new TypeReference<>() {
  };

  private final JsonMapper jsonMapper;

  public String write(TraceState state) {
    try {
      return jsonMapper.writeValueAsString(state.entries());
    } catch (JacksonException e) {
      log.error("trace_state 직렬화 실패", e);
      throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e);
    }
  }

  public TraceState read(String json) {
    try {
      Map<TraceElement, TraceStateEntry> entries = jsonMapper.readValue(json, ENTRIES_TYPE);
      if (entries == null) {
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
      }
      return new TraceState(entries);
    } catch (JacksonException e) {
      log.error("trace_state 역직렬화 실패", e);
      throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e);
    }
  }
}
