package com.chuseok22.whoim.server.common.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 도메인 에러 코드도 이 enum에 섹션을 추가해 한 곳에서 관리한다
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // 공통
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
  MALFORMED_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문 형식이 올바르지 않습니다"),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없습니다"),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않는 요청 방식입니다"),
  UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다"),
  STATE_CONFLICT(HttpStatus.CONFLICT, "현재 상태에서는 처리할 수 없는 요청입니다"),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다");

  private final HttpStatus httpStatus;
  private final String message;
}
