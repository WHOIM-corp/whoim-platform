package com.chuseok22.whoim.server.auth.application.controller;

import com.chuseok22.logging.annotation.LogMonitoring;
import com.chuseok22.whoim.server.auth.application.dto.request.LoginRequest;
import com.chuseok22.whoim.server.auth.application.dto.response.LoginResponse;
import com.chuseok22.whoim.server.auth.application.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

  private final LoginService loginService;

  @LogMonitoring
  @Override
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(loginService.login(request));
  }
}
