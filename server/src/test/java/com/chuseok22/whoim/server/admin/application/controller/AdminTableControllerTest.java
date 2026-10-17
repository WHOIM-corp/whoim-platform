package com.chuseok22.whoim.server.admin.application.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminCell;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminFieldResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminRowResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSidebarResponse;
import com.chuseok22.whoim.server.admin.application.service.AdminTableService;
import com.chuseok22.whoim.server.admin.core.constant.AdminTable;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminTableController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(AdminExceptionHandler.class)
class AdminTableControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AdminTableService adminTableService;

  @BeforeEach
  void stubSidebar() {
    given(adminTableService.sidebar()).willReturn(new AdminSidebarResponse(List.of(), 14));
  }

  @Test
  void tables_rendersSummaryView() throws Exception {
    given(adminTableService.summaries()).willReturn(List.of());
    given(adminTableService.overriddenSettingCount()).willReturn(3L);

    mockMvc.perform(get("/api/admin/tables"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/tables"))
        .andExpect(model().attributeExists("overriddenSettingCount"));
  }

  @Test
  void unknownTable_rendersNotFoundErrorView() throws Exception {
    given(adminTableService.list(eq("unknown"), eq(0), any()))
        .willThrow(new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

    mockMvc.perform(get("/api/admin/tables/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(view().name("admin/error"));
  }

  @Test
  void pageBeyondLast_rendersEmptyList() throws Exception {
    given(adminTableService.list(eq("member"), eq(999), any()))
        .willReturn(new AdminListResponse("member", List.of("id"), List.of(), 999, 1, 4, null, List.of()));

    mockMvc.perform(get("/api/admin/tables/member").param("page", "999"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/rows"));
  }

  @Test
  void nonNumericPage_returnsBadRequest() throws Exception {
    mockMvc.perform(get("/api/admin/tables/member").param("page", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("admin/error"));
  }

  @Test
  void nonUuidId_returnsBadRequest() throws Exception {
    mockMvc.perform(get("/api/admin/tables/member/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("admin/error"));
  }

  @Test
  void detail_rendersDetailView() throws Exception {
    UUID id = UUID.randomUUID();
    given(adminTableService.detail("member", id))
        .willReturn(new AdminDetailResponse("member", id, List.of(), List.of()));

    mockMvc.perform(get("/api/admin/tables/member/{id}", id))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/detail"));
  }

  @Test
  void handoverPackageId_isPassedToService() throws Exception {
    UUID packageId = UUID.randomUUID();
    given(adminTableService.list("gap", 0, packageId))
        .willReturn(new AdminListResponse("gap", List.of("id"), List.of(), 0, 0, 0, packageId, List.of()));

    mockMvc.perform(get("/api/admin/tables/gap").param("handover-package-id", packageId.toString()))
        .andExpect(status().isOk());

    verify(adminTableService).list("gap", 0, packageId);
  }

  @Test
  void rows_rendersLinkBadgeAndEmptyCells() throws Exception {
    UUID rowId = UUID.randomUUID();
    AdminListResponse list = new AdminListResponse("handover_package", List.of("id", "status", "successor_id"),
        List.of(new AdminRowResponse(rowId, List.of(
            AdminCell.link(AdminTable.HANDOVER_PACKAGE, rowId),
            AdminCell.badge("WRITING"),
            AdminCell.blank()))),
        0, 1, 1, null, List.of());
    given(adminTableService.list("handover_package", 0, null)).willReturn(list);

    String body = mockMvc.perform(get("/api/admin/tables/handover_package"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    assertThat(body).contains("WRITING").contains("/api/admin/tables/handover_package/" + rowId).contains("1 - 1 / 1");
  }

  @Test
  void detail_escapesUserText() throws Exception {
    UUID id = UUID.randomUUID();
    given(adminTableService.detail("member", id)).willReturn(new AdminDetailResponse("member", id,
        List.of(new AdminFieldResponse("name", AdminCell.text("<script>alert(1)</script>")),
            new AdminFieldResponse("position", AdminCell.blank())),
        List.of()));

    String body = mockMvc.perform(get("/api/admin/tables/member/{id}", id))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    assertThat(body).doesNotContain("<script>alert(1)</script>").contains("&lt;script&gt;");
  }
}
