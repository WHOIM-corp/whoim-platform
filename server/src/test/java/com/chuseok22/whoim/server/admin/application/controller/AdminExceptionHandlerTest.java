package com.chuseok22.whoim.server.admin.application.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@WebMvcTest(controllers = AdminExceptionHandlerTest.ProbeController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({AdminExceptionHandler.class, AdminExceptionHandlerTest.ProbeController.class})
class AdminExceptionHandlerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void customException_rendersErrorViewWithStatus() throws Exception {
    mockMvc.perform(get("/api/admin/probe/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(view().name("admin/error"))
        .andExpect(model().attribute("errorMessage", "요청한 대상을 찾을 수 없습니다"));
  }

  @Test
  void typeMismatch_rendersBadRequestErrorView() throws Exception {
    String body = mockMvc.perform(get("/api/admin/probe/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("admin/error"))
        .andReturn().getResponse().getContentAsString();

    assertThat(body).doesNotContain("Exception");
  }

  @Test
  void unexpectedException_rendersInternalServerErrorViewWithoutStack() throws Exception {
    String body = mockMvc.perform(get("/api/admin/probe/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(view().name("admin/error"))
        .andReturn().getResponse().getContentAsString();

    assertThat(body).doesNotContain("Exception").doesNotContain("secret-detail");
  }

  @Controller
  static class ProbeController {

    @GetMapping("/api/admin/probe/not-found")
    @ResponseBody
    String notFound() {
      throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @GetMapping("/api/admin/probe/boom")
    @ResponseBody
    String boom() {
      throw new IllegalStateException("secret-detail");
    }

    @GetMapping("/api/admin/probe/{id}")
    @ResponseBody
    String byId(@PathVariable UUID id) {
      return id.toString();
    }
  }
}
