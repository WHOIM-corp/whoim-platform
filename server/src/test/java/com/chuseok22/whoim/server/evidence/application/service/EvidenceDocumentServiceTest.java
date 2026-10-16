package com.chuseok22.whoim.server.evidence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.evidence.application.dto.response.EvidenceDocumentDetailResponse;
import com.chuseok22.whoim.server.evidence.application.dto.response.EvidenceDocumentResponse;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.evidence.infrastructure.properties.EvidenceProperties;
import com.chuseok22.whoim.server.evidence.infrastructure.repository.EvidenceDocumentRepository;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapRepository;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EvidenceDocumentServiceTest {

  private static final long MAX_FILE_BYTES = 131072;

  @Mock
  private HandoverPackageRepository handoverPackageRepository;
  @Mock
  private EvidenceDocumentRepository evidenceDocumentRepository;
  @Mock
  private GapRepository gapRepository;

  private EvidenceDocumentService service;

  private Member predecessor;
  private HandoverPackage handoverPackage;
  private AuthenticatedMember predecessorRequester;

  @BeforeEach
  void setUp() {
    service = new EvidenceDocumentService(
        handoverPackageRepository, evidenceDocumentRepository, gapRepository, new EvidenceProperties(MAX_FILE_BYTES));
    predecessor = givenMember("predecessor", Position.PREDECESSOR);
    handoverPackage = givenPackage(predecessor);
    predecessorRequester = requesterOf(predecessor);
  }

  @Test
  void upload_validMarkdown_savesNormalizedContent() {
    givenUploadablePackage();

    EvidenceDocumentResponse response = service.upload(
        predecessorRequester, handoverPackage.getId(), DocumentType.RUNBOOK,
        file("report.md", "\uFEFFa\r\nb".getBytes(StandardCharsets.UTF_8)));

    assertThat(savedDocument().getContent()).isEqualTo("a\nb");
    assertThat(response.characterCount()).isEqualTo(3);
    assertThat(response.fileName()).isEqualTo("report.md");
    assertThat(response.uploaderId()).isEqualTo(predecessor.getId());
    assertThat(response.uploaderName()).isEqualTo("predecessor");
  }

  @Test
  void upload_extensionRules() {
    givenUploadablePackage();

    assertThatCode(() -> upload("REPORT.MD", "a")).doesNotThrowAnyException();
    assertThatCode(() -> upload("a.TXT", "a")).doesNotThrowAnyException();
    for (String rejected : new String[]{"report.md.exe", "report.pdf", ".md", "noextension", ""}) {
      assertThat(errorCodeOf(() -> upload(rejected, "a")))
          .as(rejected).isEqualTo(ErrorCode.EVIDENCE_EXTENSION_NOT_ALLOWED);
    }
    assertThat(errorCodeOf(() -> service.upload(
        predecessorRequester, handoverPackage.getId(), DocumentType.RUNBOOK,
        new MockMultipartFile("file", null, "text/plain", new byte[]{'a'}))))
        .isEqualTo(ErrorCode.EVIDENCE_EXTENSION_NOT_ALLOWED);
  }

  @Test
  void upload_pathInFileName_isReducedToBaseName() {
    givenUploadablePackage();

    upload("../../x.txt", "a");
    upload("C:\\dir\\x.txt", "a");

    ArgumentCaptor<EvidenceDocument> captor = ArgumentCaptor.forClass(EvidenceDocument.class);
    verify(evidenceDocumentRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues()).extracting(EvidenceDocument::getFileName).containsExactly("x.txt", "x.txt");
  }

  @Test
  void upload_emptyFiles() {
    givenUploadablePackage();

    assertThat(errorCodeOf(() -> service.upload(
        predecessorRequester, handoverPackage.getId(), DocumentType.RUNBOOK, file("a.md", new byte[0]))))
        .isEqualTo(ErrorCode.EVIDENCE_FILE_EMPTY);
    assertThat(errorCodeOf(() -> upload("a.md", "  \n\t"))).isEqualTo(ErrorCode.EVIDENCE_FILE_EMPTY);
    assertThat(errorCodeOf(() -> upload("a.md", "\uFEFF"))).isEqualTo(ErrorCode.EVIDENCE_FILE_EMPTY);
  }

  @Test
  void upload_loneCarriageReturnIsKept() {
    givenUploadablePackage();

    upload("a.md", "a\rb");

    assertThat(savedDocument().getContent()).isEqualTo("a\rb");
  }

  @Test
  void upload_sizeBoundary() {
    givenUploadablePackage();

    assertThatCode(() -> upload("a.md", "a".repeat(131072))).doesNotThrowAnyException();
    assertThat(errorCodeOf(() -> upload("a.md", "a".repeat(131073))))
        .isEqualTo(ErrorCode.EVIDENCE_FILE_TOO_LARGE);
    assertThat(errorCodeOf(() -> upload("a.md", "가".repeat(43691))))
        .isEqualTo(ErrorCode.EVIDENCE_FILE_TOO_LARGE);
  }

  @Test
  void upload_invalidUtf8AndNul() {
    givenUploadablePackage();

    assertThat(errorCodeOf(() -> service.upload(
        predecessorRequester, handoverPackage.getId(), DocumentType.RUNBOOK,
        file("a.md", new byte[]{(byte) 0xFF, (byte) 0xFE, 0x41}))))
        .isEqualTo(ErrorCode.EVIDENCE_NOT_UTF8);
    assertThat(errorCodeOf(() -> upload("a.md", "a\u0000b"))).isEqualTo(ErrorCode.EVIDENCE_NOT_UTF8);
  }

  @Test
  void upload_stateAndOwnership() {
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    Member otherPredecessor = givenMember("other", Position.PREDECESSOR);
    Member reviewer = givenMember("reviewer", Position.REVIEWER);
    Member successor = givenMember("successor", Position.SUCCESSOR);

    for (Member denied : List.of(otherPredecessor, reviewer, successor)) {
      assertThat(errorCodeOf(() -> service.upload(
          requesterOf(denied), handoverPackage.getId(), DocumentType.RUNBOOK, file("a.md", "a".getBytes(StandardCharsets.UTF_8)))))
          .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    // 소유 검사가 파일 검사보다 먼저다: 잘못된 파일이어도 권한 오류가 우선한다
    assertThat(errorCodeOf(() -> service.upload(
        requesterOf(otherPredecessor), handoverPackage.getId(), DocumentType.RUNBOOK, file("x.pdf", new byte[0]))))
        .isEqualTo(ErrorCode.ACCESS_DENIED);

    given(gapRepository.existsByHandoverPackage(handoverPackage)).willReturn(true);
    assertThat(errorCodeOf(() -> upload("a.md", "a"))).isEqualTo(ErrorCode.STATE_CONFLICT);
    // 상태 검사도 파일 검사보다 먼저다
    assertThat(errorCodeOf(() -> upload("x.pdf", "a"))).isEqualTo(ErrorCode.STATE_CONFLICT);

    // PUBLISHED는 Gap 조회 전에 거부된다(Gap 조회는 위 두 번만 일어났다)
    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);
    assertThat(errorCodeOf(() -> upload("a.md", "a"))).isEqualTo(ErrorCode.STATE_CONFLICT);
    assertThat(errorCodeOf(() -> upload("x.pdf", "a"))).isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(gapRepository, times(2)).existsByHandoverPackage(any());

    UUID missingId = UUID.randomUUID();
    given(handoverPackageRepository.findById(missingId)).willReturn(Optional.empty());
    assertThat(errorCodeOf(() -> service.upload(
        predecessorRequester, missingId, DocumentType.RUNBOOK, file("a.md", "a".getBytes(StandardCharsets.UTF_8)))))
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

    verify(evidenceDocumentRepository, never()).save(any());
  }

  @Test
  void upload_admin_isAccessDeniedWithoutNullPointer() {
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    AuthenticatedMember admin = new AuthenticatedMember(UUID.randomUUID(), "admin", Role.ADMIN, null);

    assertThat(errorCodeOf(() -> service.upload(
        admin, handoverPackage.getId(), DocumentType.RUNBOOK, file("a.md", "a".getBytes(StandardCharsets.UTF_8)))))
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void list_and_get_access() {
    EvidenceDocument first = givenDocument("first.md", "one");
    EvidenceDocument second = givenDocument("second.md", "two");
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(evidenceDocumentRepository.findAllByHandoverPackageOrderByCreatedAtAsc(handoverPackage))
        .willReturn(List.of(first, second));
    given(evidenceDocumentRepository.findById(first.getId())).willReturn(Optional.of(first));
    AuthenticatedMember reviewerRequester = requesterOf(givenMember("reviewer", Position.REVIEWER));
    AuthenticatedMember otherPredecessor = requesterOf(givenMember("other", Position.PREDECESSOR));
    AuthenticatedMember successorRequester = requesterOf(givenMember("successor", Position.SUCCESSOR));

    assertThat(service.list(predecessorRequester, handoverPackage.getId()))
        .extracting(EvidenceDocumentResponse::fileName).containsExactly("first.md", "second.md");
    assertThat(service.list(reviewerRequester, handoverPackage.getId())).hasSize(2);
    EvidenceDocumentDetailResponse detail = service.get(reviewerRequester, first.getId());
    assertThat(detail.content()).isEqualTo("one");
    assertThat(service.get(predecessorRequester, first.getId()).fileName()).isEqualTo("first.md");

    assertThat(errorCodeOf(() -> service.list(otherPredecessor, handoverPackage.getId())))
        .isEqualTo(ErrorCode.ACCESS_DENIED);
    assertThat(errorCodeOf(() -> service.list(successorRequester, handoverPackage.getId())))
        .isEqualTo(ErrorCode.ACCESS_DENIED);
    assertThat(errorCodeOf(() -> service.get(otherPredecessor, first.getId())))
        .isEqualTo(ErrorCode.ACCESS_DENIED);
    assertThat(errorCodeOf(() -> service.get(successorRequester, first.getId())))
        .isEqualTo(ErrorCode.ACCESS_DENIED);

    UUID missingId = UUID.randomUUID();
    given(handoverPackageRepository.findById(missingId)).willReturn(Optional.empty());
    given(evidenceDocumentRepository.findById(missingId)).willReturn(Optional.empty());
    assertThat(errorCodeOf(() -> service.list(predecessorRequester, missingId)))
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    assertThat(errorCodeOf(() -> service.get(predecessorRequester, missingId)))
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void toAiInputText_returnsContentUnchanged() {
    EvidenceDocument document = givenDocument("a.md", "원문 내용");

    assertThat(service.toAiInputText(document)).isEqualTo("원문 내용");
  }

  private void givenUploadablePackage() {
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(gapRepository.existsByHandoverPackage(handoverPackage)).willReturn(false);
    // 파일 검증에서 거부되는 케이스는 save까지 가지 않는다
    lenient().when(evidenceDocumentRepository.save(any(EvidenceDocument.class))).then(returnsFirstArg());
  }

  private EvidenceDocumentResponse upload(String fileName, String content) {
    return service.upload(predecessorRequester, handoverPackage.getId(), DocumentType.RUNBOOK,
        file(fileName, content.getBytes(StandardCharsets.UTF_8)));
  }

  private static MockMultipartFile file(String fileName, byte[] bytes) {
    return new MockMultipartFile("file", fileName, "text/plain", bytes);
  }

  private EvidenceDocument savedDocument() {
    ArgumentCaptor<EvidenceDocument> captor = ArgumentCaptor.forClass(EvidenceDocument.class);
    verify(evidenceDocumentRepository).save(captor.capture());
    return captor.getValue();
  }

  private static ErrorCode errorCodeOf(Runnable action) {
    Throwable thrown = catchThrowable(action::run);
    assertThat(thrown).isInstanceOf(CustomException.class);
    return ((CustomException) thrown).getErrorCode();
  }

  private static Member givenMember(String name, Position position) {
    Member member = Member.builder().username(name).name(name).role(Role.USER).position(position).build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private static HandoverPackage givenPackage(Member owner) {
    HandoverPackage created = HandoverPackage.builder().predecessor(owner).name("package").build();
    ReflectionTestUtils.setField(created, "id", UUID.randomUUID());
    return created;
  }

  private EvidenceDocument givenDocument(String fileName, String content) {
    EvidenceDocument document = EvidenceDocument.builder()
        .handoverPackage(handoverPackage).fileName(fileName).documentType(DocumentType.RUNBOOK)
        .content(content).uploadedBy(predecessor).build();
    ReflectionTestUtils.setField(document, "id", UUID.randomUUID());
    return document;
  }

  private static AuthenticatedMember requesterOf(Member member) {
    return new AuthenticatedMember(member.getId(), member.getUsername(), member.getRole(), member.getPosition());
  }
}
