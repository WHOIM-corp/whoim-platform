package com.chuseok22.whoim.server.admin.application.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class AdminLoginController {

  @GetMapping("/api/admin/login")
  public String loginPage() {
    return "admin/login";
  }
}
