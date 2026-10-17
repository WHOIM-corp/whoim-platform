package com.chuseok22.whoim.server.admin.application.controller;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminSidebarResponse;
import com.chuseok22.whoim.server.admin.application.service.AdminSettingService;
import com.chuseok22.whoim.server.admin.application.service.AdminTableService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Hidden
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/admin/settings")
public class AdminSettingController {

  private final AdminSettingService adminSettingService;
  private final AdminTableService adminTableService;

  @ModelAttribute("sidebar")
  public AdminSidebarResponse sidebar() {
    return adminTableService.sidebar();
  }

  @GetMapping
  public String settings(Model model) {
    model.addAttribute("groups", adminSettingService.groups());
    return "admin/settings";
  }

  // 설정 키에 점이 있어 확장자로 해석되지 않도록 정규식으로 받는다. 빈 값은 오버라이드 삭제다
  @PostMapping("/{setting-config-key:.+}")
  public String save(
      @PathVariable("setting-config-key") String settingConfigKey,
      @RequestParam(defaultValue = "") String value,
      Model model
  ) {
    model.addAttribute("setting", adminSettingService.save(settingConfigKey, value));
    return "admin/settings :: row";
  }
}
