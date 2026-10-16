package com.chuseok22.whoim.server.evidence.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAccessDeniedHandler;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtAuthenticationEntryPoint;
import com.chuseok22.whoim.server.auth.infrastructure.config.JwtProvider;
import com.chuseok22.whoim.server.auth.infrastructure.config.SecurityConfig;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.common.application.exception.GlobalExceptionHandler;
import com.chuseok22.whoim.server.evidence.application.dto.response.EvidenceDocumentDetailResponse;
import com.chuseok22.whoim.server.evidence.application.dto.response.EvidenceDocumentResponse;
import com.chuseok22.whoim.server.evidence.application.service.EvidenceDocumentService;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@WebMvcTest(controllers = EvidenceDocumentController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "jwt.secret-key=security-slice-dummy-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz-ABCDEFG",
    "jwt.access-exp-millis=3600000",
    "jwt.issuer=test-issuer"
})
// Docs 인터페이스 + @PreAuthorize 조합이 JDK 프록시가 되어 슬라이스에서 매핑이 사라지는 것을 막는다
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({
    SecurityConfig.class,
    JwtProvider.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class,
    GlobalExceptionHandler.class,
    EvidenceDocumentControllerTest.FixedClockConfig.class
})
class EvidenceDocumentControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");
  private static final String BASE_URL = "/api/evidence-documents";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @MockitoBean
  private EvidenceDocumentService evidenceDocumentService;

  @Test
  void list_asReviewer_isNotNotFound() throws Exception {
    given(evidenceDocumentService.list(any(), any())).willReturn(List.of());

    mockMvc.perform(get(BASE_URL)
            .param("handoverPackageId", UUID.randomUUID().toString())
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, Position.REVIEWER)))
        .andExpect(status().isOk());
  }

  @Test
  void upload_asPredecessor_returns201WithMeta() throws Exception {
    UUID documentId = UUID.randomUUID();
    given(evidenceDocumentService.upload(any(), any(), eq(DocumentType.RUNBOOK), any()))
        .willReturn(new EvidenceDocumentResponse(documentId, "a.md", DocumentType.RUNBOOK, 3,
            UUID.randomUUID(), "전임자", OffsetDateTime.of(2026, 10, 15, 9, 0, 0, 0, ZoneOffset.ofHours(9))));

    mockMvc.perform(uploadRequest(Role.USER, Position.PREDECESSOR))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(documentId.toString()))
        .andExpect(jsonPath("$.fileName").value("a.md"))
        .andExpect(jsonPath("$.uploadedAt").value(org.hamcrest.Matchers.endsWith("+09:00")));
  }

  @Test
  void upload_asReviewerSuccessorAdmin_returns403() throws Exception {
    mockMvc.perform(uploadRequest(Role.USER, Position.REVIEWER))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    mockMvc.perform(uploadRequest(Role.USER, Position.SUCCESSOR))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    mockMvc.perform(uploadRequest(Role.ADMIN, null))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

    verifyNoInteractions(evidenceDocumentService);
  }

  @Test
  void upload_missingFilePart_returns400InvalidRequest() throws Exception {
    mockMvc.perform(multipart(BASE_URL)
            .param("handoverPackageId", UUID.randomUUID().toString())
            .param("documentType", "RUNBOOK")
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, Position.PREDECESSOR)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
  }

  @Test
  void upload_invalidDocumentType_returns400InvalidRequest() throws Exception {
    mockMvc.perform(multipart(BASE_URL)
            .file(markdownFile())
            .param("handoverPackageId", UUID.randomUUID().toString())
            .param("documentType", "NOT_A_TYPE")
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, Position.PREDECESSOR)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
  }

  @Test
  void upload_missingHandoverPackageId_returns400InvalidRequest() throws Exception {
    mockMvc.perform(multipart(BASE_URL)
            .file(markdownFile())
            .param("documentType", "RUNBOOK")
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, Position.PREDECESSOR)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
  }

  @Test
  void upload_noToken_returns401() throws Exception {
    mockMvc.perform(multipart(BASE_URL)
            .file(markdownFile())
            .param("handoverPackageId", UUID.randomUUID().toString())
            .param("documentType", "RUNBOOK"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
  }

  @Test
  void list_and_get() throws Exception {
    UUID documentId = UUID.randomUUID();
    given(evidenceDocumentService.list(any(), any())).willReturn(List.of());
    given(evidenceDocumentService.get(any(), eq(documentId)))
        .willReturn(new EvidenceDocumentDetailResponse(documentId, "a.md", DocumentType.RUNBOOK, 3,
            UUID.randomUUID(), "전임자", OffsetDateTime.of(2026, 10, 15, 9, 0, 0, 0, ZoneOffset.ofHours(9)), "a\nb"));
    String packageId = UUID.randomUUID().toString();

    for (Position position : List.of(Position.PREDECESSOR, Position.REVIEWER)) {
      mockMvc.perform(get(BASE_URL).param("handoverPackageId", packageId)
              .header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, position)))
          .andExpect(status().isOk());
      mockMvc.perform(get(BASE_URL + "/" + documentId)
              .header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, position)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").value("a\nb"));
    }

    mockMvc.perform(get(BASE_URL).param("handoverPackageId", packageId)
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, Position.SUCCESSOR)))
        .andExpect(status().isForbidden());
    mockMvc.perform(get(BASE_URL).param("handoverPackageId", packageId)
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN, null)))
        .andExpect(status().isForbidden());
    mockMvc.perform(get(BASE_URL + "/" + documentId)
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.USER, Position.SUCCESSOR)))
        .andExpect(status().isForbidden());
    mockMvc.perform(get(BASE_URL + "/" + documentId)
            .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN, null)))
        .andExpect(status().isForbidden());
  }

  @Test
  void list_invalidOrMissingHandoverPackageId_returns400InvalidRequest() throws Exception {
    String token = bearer(Role.USER, Position.REVIEWER);

    mockMvc.perform(get(BASE_URL).param("handoverPackageId", "not-a-uuid")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    mockMvc.perform(get(BASE_URL).header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
  }

  @Test
  void serviceErrorsAreMappedToErrorResponse() throws Exception {
    given(evidenceDocumentService.upload(any(), any(), any(), any()))
        .willThrow(new CustomException(ErrorCode.STATE_CONFLICT))
        .willThrow(new CustomException(ErrorCode.EVIDENCE_FILE_TOO_LARGE));

    mockMvc.perform(uploadRequest(Role.USER, Position.PREDECESSOR))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("STATE_CONFLICT"));
    mockMvc.perform(uploadRequest(Role.USER, Position.PREDECESSOR))
        .andExpect(status().isContentTooLarge())
        .andExpect(jsonPath("$.errorCode").value("EVIDENCE_FILE_TOO_LARGE"));
  }

  private MockMultipartHttpServletRequestBuilder uploadRequest(Role role, Position position) {
    MockMultipartHttpServletRequestBuilder builder = multipart(BASE_URL).file(markdownFile());
    builder.param("handoverPackageId", UUID.randomUUID().toString());
    builder.param("documentType", "RUNBOOK");
    builder.header(HttpHeaders.AUTHORIZATION, bearer(role, position));
    return builder;
  }

  private MockMultipartFile markdownFile() {
    return new MockMultipartFile("file", "a.md", "text/markdown", "a\nb".getBytes(StandardCharsets.UTF_8));
  }

  private String bearer(Role role, Position position) {
    return "Bearer " + jwtProvider.issue(new AuthenticatedMember(UUID.randomUUID(), "probe-user", role, position));
  }

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
