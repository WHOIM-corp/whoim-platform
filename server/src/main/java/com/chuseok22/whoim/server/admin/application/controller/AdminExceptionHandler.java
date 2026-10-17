package com.chuseok22.whoim.server.admin.application.controller;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

// 주입 의존성을 두지 않는다. @WebMvcTest가 모든 @ControllerAdvice를 스캔하므로 의존성이 있으면 기존 슬라이스 테스트가 깨진다
@Slf4j
@ControllerAdvice(basePackageClasses = AdminLoginController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminExceptionHandler {

  private static final String ERROR_VIEW = "admin/error";

  @ExceptionHandler(CustomException.class)
  public ModelAndView handleCustomException(CustomException e) {
    return errorView(e.getErrorCode());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ModelAndView handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    return errorView(ErrorCode.INVALID_REQUEST);
  }

  // Security 필터 체인이 로그인 이동·403을 처리하도록 다시 던진다
  @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
  public void rethrowSecurityException(Exception e) throws Exception {
    throw e;
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView handleUnexpected(Exception e) {
    log.error("관리자 화면 처리되지 않은 예외", e);
    return errorView(ErrorCode.INTERNAL_SERVER_ERROR);
  }

  private ModelAndView errorView(ErrorCode errorCode) {
    if (errorCode.getHttpStatus().is5xxServerError()) {
      log.error("관리자 화면 서버 오류: {}", errorCode);
    } else {
      log.warn("관리자 화면 요청 실패: {}", errorCode);
    }
    ModelAndView modelAndView = new ModelAndView(ERROR_VIEW);
    modelAndView.setStatus(errorCode.getHttpStatus());
    modelAndView.addObject("errorMessage", errorCode.getMessage());
    return modelAndView;
  }
}
