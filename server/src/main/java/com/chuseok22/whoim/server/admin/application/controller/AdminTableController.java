package com.chuseok22.whoim.server.admin.application.controller;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminSidebarResponse;
import com.chuseok22.whoim.server.admin.application.service.AdminTableService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Hidden
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/admin/tables")
public class AdminTableController {

  private final AdminTableService adminTableService;

  @ModelAttribute("sidebar")
  public AdminSidebarResponse sidebar() {
    return adminTableService.sidebar();
  }

  @GetMapping
  public String tables(Model model) {
    model.addAttribute("tables", adminTableService.summaries());
    model.addAttribute("overriddenSettingCount", adminTableService.overriddenSettingCount());
    return "admin/tables";
  }

  @GetMapping("/{table-name}")
  public String rows(
      @PathVariable("table-name") String tableName,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(name = "handover-package-id", required = false) UUID handoverPackageId,
      Model model
  ) {
    model.addAttribute("list", adminTableService.list(tableName, page, handoverPackageId));
    return "admin/rows";
  }

  @GetMapping("/{table-name}/{id}")
  public String detail(
      @PathVariable("table-name") String tableName,
      @PathVariable UUID id,
      Model model
  ) {
    model.addAttribute("detail", adminTableService.detail(tableName, id));
    return "admin/detail";
  }
}
