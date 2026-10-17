package com.chuseok22.whoim.server.admin.application.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminCell;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminFieldResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminRowResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSectionResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSidebarResponse;
import com.chuseok22.whoim.server.admin.application.service.AdminTableService;
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
class AdminDetailRenderingTest {

  private static final String SCRIPT = "<script>alert(1)</script>";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AdminTableService adminTableService;

  @BeforeEach
  void stubSidebar() {
    given(adminTableService.sidebar()).willReturn(new AdminSidebarResponse(List.of(), 14));
  }

  @Test
  void detail_escapesDocumentContent() throws Exception {
    UUID id = UUID.randomUUID();
    given(adminTableService.detail("evidence_document", id)).willReturn(new AdminDetailResponse(
        "evidence_document", id, List.of(new AdminFieldResponse("content", AdminCell.text(SCRIPT))), List.of()));

    String body = mockMvc.perform(get("/api/admin/tables/evidence_document/{id}", id))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    assertThat(body).doesNotContain(SCRIPT).contains("&lt;script&gt;");
  }

  @Test
  void detail_escapesAnchorExcerptInSection() throws Exception {
    UUID id = UUID.randomUUID();
    AdminSectionResponse section = new AdminSectionResponse("근거 앵커 (gap_anchor)", "TABLE", List.of("excerpt"),
        List.of(new AdminRowResponse(UUID.randomUUID(), List.of(AdminCell.text(SCRIPT)))));
    given(adminTableService.detail("gap", id))
        .willReturn(new AdminDetailResponse("gap", id, List.of(), List.of(section)));

    String body = mockMvc.perform(get("/api/admin/tables/gap/{id}", id))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    assertThat(body).doesNotContain(SCRIPT).contains("&lt;script&gt;");
  }

  @Test
  void detail_rendersConversationTurnsAndEscapesAnswer() throws Exception {
    UUID id = UUID.randomUUID();
    AdminSectionResponse section = new AdminSectionResponse("턴 (interview_turn)", "CONVERSATION",
        List.of("turn"), List.of(
            new AdminRowResponse(UUID.randomUUID(), List.of(
                AdminCell.text("TURN 1 · 0f3a9c21 · 2026-10-18 09:00:00"),
                AdminCell.text("어떤 신호를 보셨나요?"),
                AdminCell.text(SCRIPT),
                AdminCell.text("{\"signal\":\"지연\"}"))),
            new AdminRowResponse(UUID.randomUUID(), List.of(
                AdminCell.text("TURN 2 · 1a2b3c4d · 2026-10-18 09:01:00"),
                AdminCell.text("그다음 판단은?"),
                AdminCell.text("(답변 없음)"),
                AdminCell.blank()))));
    given(adminTableService.detail("interview", id))
        .willReturn(new AdminDetailResponse("interview", id, List.of(), List.of(section)));

    String body = mockMvc.perform(get("/api/admin/tables/interview/{id}", id))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    assertThat(body)
        .contains("TURN 1 · 0f3a9c21 · 2026-10-18 09:00:00")
        .contains("어떤 신호를 보셨나요?")
        .contains("TURN 2 · 1a2b3c4d")
        .contains("(답변 없음)")
        .contains("&lt;script&gt;")
        .doesNotContain(SCRIPT);
  }
}
