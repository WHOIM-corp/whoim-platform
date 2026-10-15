package com.chuseok22.whoim.server.common.application.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ProbeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandlerTest.ProbeController.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void customException_returnsStatusAndCodeWithoutHttpStatusField() throws Exception {
    mockMvc.perform(get("/probe/custom"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("STATE_CONFLICT"))
        .andExpect(jsonPath("$.errorMessage").value("현재 상태에서는 처리할 수 없는 요청입니다"))
        .andExpect(jsonPath("$.httpStatus").doesNotExist());
  }

  @Test
  void malformedJson_returns400MalformedRequestBody() throws Exception {
    mockMvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{broken"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST_BODY"));
  }

  @Test
  void unsupportedMethod_returns405WithAllowHeader() throws Exception {
    mockMvc.perform(post("/probe/custom"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", "GET"))
        .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  void unsupportedMediaType_returns415() throws Exception {
    mockMvc.perform(post("/probe/body").contentType(MediaType.TEXT_PLAIN).content("x"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));
  }

  @Test
  void unknownPath_returns404() throws Exception {
    mockMvc.perform(get("/probe/none"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void unexpectedException_returns500WithoutInternalDetails() throws Exception {
    mockMvc.perform(get("/probe/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.errorMessage").value("서버에 문제가 발생했습니다"))
        .andExpect(result -> assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
            .doesNotContain("secret-internal-detail"));
  }

  @Test
  void securityException_isRethrownInsteadOfBecoming500() {
    assertThatThrownBy(() -> mockMvc.perform(get("/probe/denied")))
        .hasCauseInstanceOf(AccessDeniedException.class);
  }

  @Test
  void offsetDateTime_isSerializedWithPlusNineOffset() throws Exception {
    mockMvc.perform(get("/probe/time"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.occurredAt").value("2026-10-13T10:00:00+09:00"));
  }

  record TimeResponse(OffsetDateTime occurredAt) {
  }

  @RestController
  static class ProbeController {

    @GetMapping("/probe/custom")
    void custom() {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }

    @PostMapping("/probe/body")
    void body(@RequestBody Map<String, String> body) {
    }

    @GetMapping("/probe/boom")
    void boom() {
      throw new IllegalStateException("secret-internal-detail");
    }

    @GetMapping("/probe/denied")
    void denied() {
      throw new AccessDeniedException("denied");
    }

    @GetMapping("/probe/time")
    TimeResponse time() {
      return new TimeResponse(OffsetDateTime.of(2026, 10, 13, 10, 0, 0, 0, ZoneOffset.ofHours(9)));
    }
  }
}
