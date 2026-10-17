package com.chuseok22.whoim.server.admin.application.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminSettingGroupResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSettingResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSidebarResponse;
import com.chuseok22.whoim.server.admin.application.service.AdminSettingService;
import com.chuseok22.whoim.server.admin.application.service.AdminTableService;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import com.chuseok22.whoim.server.setting.core.constant.SettingValueType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminSettingController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(AdminExceptionHandler.class)
class AdminSettingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AdminSettingService adminSettingService;

  @MockitoBean
  private AdminTableService adminTableService;

  @BeforeEach
  void stubSidebar() {
    given(adminTableService.sidebar()).willReturn(new AdminSidebarResponse(List.of(), 14));
  }

  @Test
  void settings_rendersPromptTextareaAndResetButton() throws Exception {
    given(adminSettingService.groups()).willReturn(List.of(
        new AdminSettingGroupResponse("프롬프트 지침", List.of(setting(SettingKey.PROMPT_GAP_ANALYSIS, "지침", null)))));

    String body = mockMvc.perform(get("/api/admin/settings"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/settings"))
        .andExpect(model().attributeExists("groups"))
        .andReturn().getResponse().getContentAsString();

    assertThat(body).contains("<textarea").contains("기본값으로 되돌리기").contains("기본 프롬프트 보기");
  }

  @Test
  void save_rendersRowFragmentWithModel() throws Exception {
    given(adminSettingService.save("llm.gemini.temperature", "0.4"))
        .willReturn(setting(SettingKey.LLM_GEMINI_TEMPERATURE, "0.4", null));

    mockMvc.perform(post("/api/admin/settings/llm.gemini.temperature").param("value", "0.4"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/settings :: row"))
        .andExpect(model().attributeExists("setting"));
  }

  @Test
  void save_showsErrorMessageUnderInput() throws Exception {
    given(adminSettingService.save("gap.analysis.max-input-characters", "2000000"))
        .willReturn(setting(SettingKey.GAP_ANALYSIS_MAX_INPUT_CHARACTERS, "2000000",
            ErrorCode.SETTING_VALUE_INVALID.getMessage()));

    String body = mockMvc.perform(post("/api/admin/settings/gap.analysis.max-input-characters")
            .param("value", "2000000"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    assertThat(body).contains(ErrorCode.SETTING_VALUE_INVALID.getMessage());
  }

  @Test
  void save_unknownKeyRendersNotFoundErrorView() throws Exception {
    given(adminSettingService.save("unknown.key", "1"))
        .willThrow(new CustomException(ErrorCode.SETTING_KEY_NOT_FOUND));

    mockMvc.perform(post("/api/admin/settings/unknown.key").param("value", "1"))
        .andExpect(status().isNotFound())
        .andExpect(view().name("admin/error"));
  }

  private static AdminSettingResponse setting(SettingKey key, String effectiveValue, String errorMessage) {
    boolean decimalOrInteger = key.valueType() == SettingValueType.DECIMAL
        || key.valueType() == SettingValueType.INTEGER;
    return new AdminSettingResponse(key.key(), key.description(), key.valueType(), false, effectiveValue,
        key.defaultValue(), null, decimalOrInteger ? key.minimum() : 0, decimalOrInteger ? key.maximum() : 0,
        key.maxLength(), errorMessage);
  }
}
