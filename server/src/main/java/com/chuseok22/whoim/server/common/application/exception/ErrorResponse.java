package com.chuseok22.whoim.server.common.application.exception;

// HTTP 상태는 응답 상태 코드로 전달하므로 본문에는 두 필드만 둔다
public record ErrorResponse(String errorCode, String errorMessage) {

  public static ErrorResponse of(ErrorCode errorCode) {
    return new ErrorResponse(errorCode.name(), errorCode.getMessage());
  }

  public static ErrorResponse of(ErrorCode errorCode, String errorMessage) {
    return new ErrorResponse(errorCode.name(), errorMessage);
  }
}
