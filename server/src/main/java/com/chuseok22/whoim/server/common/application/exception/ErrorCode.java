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
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다"),

  // 인증
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
  EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다"),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),

  // Evidence
  EVIDENCE_FILE_EMPTY(HttpStatus.BAD_REQUEST, "빈 파일은 업로드할 수 없습니다"),
  EVIDENCE_EXTENSION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "md 또는 txt 파일만 업로드할 수 있습니다"),
  EVIDENCE_NOT_UTF8(HttpStatus.BAD_REQUEST, "UTF-8 텍스트 파일만 업로드할 수 있습니다"),
  EVIDENCE_FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 허용 범위를 초과했습니다"),
  EVIDENCE_EXCERPT_RANGE_INVALID(HttpStatus.BAD_REQUEST, "발췌 범위가 올바르지 않습니다"),

  // Gap
  GAP_ANALYSIS_NO_DOCUMENTS(HttpStatus.BAD_REQUEST, "분석할 문서가 없습니다"),
  GAP_ANALYSIS_INPUT_TOO_LARGE(HttpStatus.BAD_REQUEST, "분석할 문서 분량이 상한을 초과했습니다"),

  // LLM
  LLM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 응답을 가져올 수 없습니다"),

  // DLP
  DLP_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "민감정보 검사를 완료할 수 없어 요청을 처리하지 못했습니다"),

  // Setting
  SETTING_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "등록되지 않은 설정 키입니다"),
  SETTING_VALUE_INVALID(HttpStatus.BAD_REQUEST, "설정 값이 올바르지 않습니다");

  private final HttpStatus httpStatus;
  private final String message;
}
