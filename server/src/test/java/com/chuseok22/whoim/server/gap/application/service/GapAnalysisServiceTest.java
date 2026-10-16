package com.chuseok22.whoim.server.gap.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.evidence.application.service.EvidenceDocumentService;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.evidence.infrastructure.repository.EvidenceDocumentRepository;
import com.chuseok22.whoim.server.gap.application.dto.data.GapAnalysisResult;
import com.chuseok22.whoim.server.gap.application.dto.data.GapAnchorCandidate;
import com.chuseok22.whoim.server.gap.application.dto.data.GapCandidate;
import com.chuseok22.whoim.server.gap.application.dto.response.GapResponse;
import com.chuseok22.whoim.server.gap.core.constant.GapProgress;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapAnchorRepository;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapRepository;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.service.LlmService;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GapAnalysisServiceTest {

  @Mock
  private HandoverPackageRepository handoverPackageRepository;
  @Mock
  private EvidenceDocumentRepository evidenceDocumentRepository;
  @Mock
  private EvidenceDocumentService evidenceDocumentService;
  @Mock
  private GapRepository gapRepository;
  @Mock
  private GapAnchorRepository gapAnchorRepository;
  @Mock
  private LlmService llmService;
  @Mock
  private SettingConfigService settingConfigService;

  private GapAnalysisService service;
  private Member owner;
  private HandoverPackage handoverPackage;
  private EvidenceDocument document;

  @BeforeEach
  void setUp() {
    service = new GapAnalysisService(
        handoverPackageRepository, evidenceDocumentRepository, gapRepository, gapAnchorRepository,
        new GapAnalysisInputBuilder(evidenceDocumentService),
        new GapAnalysisResultConverter(new GapAnchorResolver()),
        llmService, settingConfigService, mock(PlatformTransactionManager.class));
    owner = member("owner", Position.PREDECESSOR);
    handoverPackage = HandoverPackage.builder().predecessor(owner).name("package").build();
    ReflectionTestUtils.setField(handoverPackage, "id", UUID.randomUUID());
    document = EvidenceDocument.builder().handoverPackage(handoverPackage).fileName("a.md")
        .documentType(DocumentType.OTHER).content("line1\nline2\nline3").build();
    ReflectionTestUtils.setField(document, "id", UUID.randomUUID());

    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(handoverPackageRepository.findWithLockById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(evidenceDocumentRepository.findAllByHandoverPackageOrderByCreatedAtAsc(handoverPackage)).willReturn(List.of(document));
    given(evidenceDocumentRepository.countByHandoverPackage(handoverPackage)).willReturn(1L);
    given(evidenceDocumentService.toAiInputText(document)).willReturn(document.getContent());
    given(settingConfigService.getInt(anyString(), anyInt())).willReturn(20000);
    given(gapRepository.save(any(Gap.class))).willAnswer(invocation -> {
      Gap gap = invocation.getArgument(0);
      ReflectionTestUtils.setField(gap, "id", UUID.randomUUID());
      return gap;
    });
    given(gapAnchorRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void analyze_savesGapsAndAnchorsAndCallsLlmOnce() {
    given(llmService.generate(any(LlmRequest.class), eq(GapAnalysisResult.class))).willReturn(resultOf(candidate("CHOICE_GAP", 2, 3)));

    List<GapResponse> responses = service.analyze(requesterOf(owner), handoverPackage.getId());

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).type()).isEqualTo(GapType.CHOICE_GAP);
    assertThat(responses.get(0).progress()).isEqualTo(GapProgress.NOT_STARTED);
    assertThat(responses.get(0).anchors()).hasSize(1);
    assertThat(responses.get(0).anchors().get(0).fileName()).isEqualTo("a.md");
    assertThat(responses.get(0).anchors().get(0).excerpt()).isEqualTo("line2\nline3");
    verify(llmService, times(1)).generate(any(LlmRequest.class), eq(GapAnalysisResult.class));
    verify(gapRepository, times(1)).save(any(Gap.class));
    verify(gapAnchorRepository, times(1)).saveAll(anyList());
  }

  @Test
  void analyze_rejectsTooLargeInputWithoutCallingLlm() {
    given(settingConfigService.getInt(anyString(), anyInt())).willReturn(10);

    CustomException exception = analyzeAndCatch(requesterOf(owner));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GAP_ANALYSIS_INPUT_TOO_LARGE);
    verifyNoInteractions(llmService);
  }

  @Test
  void analyze_rejectsNoDocuments() {
    given(evidenceDocumentRepository.findAllByHandoverPackageOrderByCreatedAtAsc(handoverPackage)).willReturn(List.of());

    CustomException exception = analyzeAndCatch(requesterOf(owner));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GAP_ANALYSIS_NO_DOCUMENTS);
    verifyNoInteractions(llmService);
  }

  @Test
  void analyze_rejectsExistingGap() {
    given(gapRepository.existsByHandoverPackage(handoverPackage)).willReturn(true);

    CustomException exception = analyzeAndCatch(requesterOf(owner));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verifyNoInteractions(llmService);
  }

  @Test
  void analyze_rejectsPublished() {
    ReflectionTestUtils.setField(handoverPackage, "status", com.chuseok22.whoim.server.handover.core.constant.HandoverStatus.PUBLISHED);

    CustomException exception = analyzeAndCatch(requesterOf(owner));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verifyNoInteractions(llmService);
  }

  @Test
  void analyze_rejectsNonOwnerAndNonPredecessor() {
    Member otherPredecessor = member("other", Position.PREDECESSOR);
    Member reviewer = member("reviewer", Position.REVIEWER);

    assertThat(analyzeAndCatch(requesterOf(otherPredecessor)).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    assertThat(analyzeAndCatch(requesterOf(reviewer)).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    verifyNoInteractions(llmService);
  }

  @Test
  void analyze_unknownPackage() {
    UUID unknownId = UUID.randomUUID();
    given(handoverPackageRepository.findById(unknownId)).willReturn(Optional.empty());

    Throwable thrown = catchThrowable(() -> service.analyze(requesterOf(owner), unknownId));

    assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void analyze_llmFailureSavesNothing() {
    given(llmService.generate(any(LlmRequest.class), eq(GapAnalysisResult.class)))
        .willThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));

    CustomException exception = analyzeAndCatch(requesterOf(owner));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);
    verify(gapRepository, never()).save(any(Gap.class));
    verify(gapAnchorRepository, never()).saveAll(anyList());
    verify(handoverPackageRepository, never()).findWithLockById(any());
  }

  @Test
  void analyze_conflictAfterLockDiscardsResult() {
    given(llmService.generate(any(LlmRequest.class), eq(GapAnalysisResult.class))).willReturn(resultOf(candidate("CHOICE_GAP", 1, 1)));
    // 읽기 단계에서는 Gap이 없고, 잠금을 잡은 뒤에는 다른 요청이 이미 저장해 둔 상황
    given(gapRepository.existsByHandoverPackage(handoverPackage)).willReturn(false, true);

    CustomException exception = analyzeAndCatch(requesterOf(owner));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(handoverPackageRepository, times(1)).findWithLockById(handoverPackage.getId());
    verify(gapRepository, never()).save(any(Gap.class));
    verify(gapAnchorRepository, never()).saveAll(anyList());
  }

  @Test
  void analyze_documentAddedDuringLlmCallConflicts() {
    given(llmService.generate(any(LlmRequest.class), eq(GapAnalysisResult.class))).willReturn(resultOf(candidate("CHOICE_GAP", 1, 1)));
    given(evidenceDocumentRepository.countByHandoverPackage(handoverPackage)).willReturn(2L);

    CustomException exception = analyzeAndCatch(requesterOf(owner));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(gapRepository, never()).save(any(Gap.class));
  }

  @Test
  void analyze_emptyConvertedResultSavesNothing() {
    given(llmService.generate(any(LlmRequest.class), eq(GapAnalysisResult.class))).willReturn(resultOf(candidate("CHOICE_GAP", 9, 9)));

    List<GapResponse> responses = service.analyze(requesterOf(owner), handoverPackage.getId());

    assertThat(responses).isEmpty();
    verify(gapRepository, never()).save(any(Gap.class));
    verify(gapAnchorRepository, never()).saveAll(anyList());
  }

  private CustomException analyzeAndCatch(AuthenticatedMember requester) {
    Throwable thrown = catchThrowable(() -> service.analyze(requester, handoverPackage.getId()));
    assertThat(thrown).isInstanceOf(CustomException.class);
    return (CustomException) thrown;
  }

  private static GapCandidate candidate(String type, int startLine, int endLine) {
    return new GapCandidate(type, "요약", "질문", List.of(new GapAnchorCandidate("D1", startLine, endLine)));
  }

  private static GapAnalysisResult resultOf(GapCandidate... candidates) {
    return new GapAnalysisResult(List.of(candidates));
  }

  private static Member member(String name, Position position) {
    Member member = Member.builder().username(name).name(name).role(Role.USER).position(position).build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private static AuthenticatedMember requesterOf(Member member) {
    return new AuthenticatedMember(member.getId(), member.getUsername(), member.getRole(), member.getPosition());
  }
}
