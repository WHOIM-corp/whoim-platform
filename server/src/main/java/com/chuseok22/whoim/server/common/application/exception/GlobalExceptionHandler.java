package com.chuseok22.whoim.server.common.application.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// ResponseEntityExceptionHandler를 상속하면 ProblemDetail 본문이 나가므로 ErrorResponse로 통일하려고 직접 매핑한다
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
    ErrorCode errorCode = e.getErrorCode();
    if (errorCode.getHttpStatus().is5xxServerError()) {
      log.error("서버 오류 예외: {}", errorCode, e);
    } else {
      log.warn("요청 처리 실패: {} - {}", errorCode, e.getMessage());
    }
    return toResponse(ErrorResponse.of(errorCode), errorCode);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
    String message = Optional.ofNullable(e.getBindingResult().getFieldError())
        .map(FieldError::getDefaultMessage)
        .orElse(ErrorCode.INVALID_REQUEST.getMessage());
    return invalidRequest(message);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException e) {
    String message = e.getAllErrors().stream()
        .map(MessageSourceResolvable::getDefaultMessage)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(ErrorCode.INVALID_REQUEST.getMessage());
    return invalidRequest(message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
    String message = e.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(ErrorCode.INVALID_REQUEST.getMessage());
    return invalidRequest(message);
  }

  @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
  public ResponseEntity<ErrorResponse> handleInvalidParameter(Exception e) {
    log.warn("요청 파라미터 오류: {}", e.getMessage());
    return toResponse(ErrorResponse.of(ErrorCode.INVALID_REQUEST), ErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException e) {
    log.warn("요청 본문 파싱 실패: {}", e.getMessage());
    return toResponse(ErrorResponse.of(ErrorCode.MALFORMED_REQUEST_BODY), ErrorCode.MALFORMED_REQUEST_BODY);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
    return toResponse(ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND), ErrorCode.RESOURCE_NOT_FOUND);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
    ResponseEntity.BodyBuilder builder = ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus());
    if (e.getSupportedHttpMethods() != null && !e.getSupportedHttpMethods().isEmpty()) {
      HttpHeaders headers = new HttpHeaders();
      headers.setAllow(e.getSupportedHttpMethods());
      builder.headers(headers);
    }
    return builder.body(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
    return toResponse(ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE), ErrorCode.UNSUPPORTED_MEDIA_TYPE);
  }

  // 인증·인가 예외는 캐치올이 500으로 삼키지 않도록 그대로 던져 Security가 처리하게 한다(#2에서 401/403 ErrorResponse 처리로 교체)
  @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
  public void rethrowSecurityException(Exception e) throws Exception {
    throw e;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
    log.error("처리되지 않은 예외", e);
    return toResponse(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR), ErrorCode.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ErrorResponse> invalidRequest(String message) {
    log.warn("요청 검증 실패: {}", message);
    return toResponse(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message), ErrorCode.INVALID_REQUEST);
  }

  private ResponseEntity<ErrorResponse> toResponse(ErrorResponse body, ErrorCode errorCode) {
    return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
  }
}
