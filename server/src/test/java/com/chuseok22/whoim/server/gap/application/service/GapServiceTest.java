package com.chuseok22.whoim.server.gap.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.TraceCardRepository;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.gap.application.dto.response.GapResponse;
import com.chuseok22.whoim.server.gap.core.constant.GapProgress;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapAnchorRepository;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapRepository;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.interview.core.constant.InterviewEndReason;
import com.chuseok22.whoim.server.interview.core.constant.InterviewStatus;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewRepository;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GapServiceTest {

  @Mock
  private HandoverPackageRepository handoverPackageRepository;
  @Mock
  private GapRepository gapRepository;
  @Mock
  private GapAnchorRepository gapAnchorRepository;
  @Mock
  private InterviewRepository interviewRepository;
  @Mock
  private TraceCardRepository traceCardRepository;

  private GapService service;
  private Member owner;
  private HandoverPackage handoverPackage;
  private EvidenceDocument document;

  @BeforeEach
  void setUp() {
    service = new GapService(handoverPackageRepository, gapRepository, gapAnchorRepository, interviewRepository, traceCardRepository);
    owner = member("owner", Position.PREDECESSOR);
    handoverPackage = HandoverPackage.builder().predecessor(owner).name("package").build();
    ReflectionTestUtils.setField(handoverPackage, "id", UUID.randomUUID());
    document = EvidenceDocument.builder().handoverPackage(handoverPackage).fileName("a.md")
        .documentType(DocumentType.OTHER).content("x").build();
    ReflectionTestUtils.setField(document, "id", UUID.randomUUID());
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
  }

  @Test
  void list_computesProgressForEachCase() {
    Gap notStarted = gap();
    Gap inProgress = gap();
    Gap completed = gap();
    Gap partial = gap();
    Gap rejected = gap();
    Interview inProgressInterview = interview(inProgress, InterviewStatus.IN_PROGRESS, null);
    Interview completedInterview = interview(completed, InterviewStatus.COMPLETED, InterviewEndReason.CHECKLIST_COMPLETE);
    Interview partialInterview = interview(partial, InterviewStatus.COMPLETED, InterviewEndReason.TURN_LIMIT);
    // REJECTED가 COMPLETED(체크리스트 완료)보다 우선한다
    Interview rejectedInterview = interview(rejected, InterviewStatus.COMPLETED, InterviewEndReason.CHECKLIST_COMPLETE);
    given(gapRepository.findAllByHandoverPackageOrderByCreatedAtAsc(handoverPackage))
        .willReturn(List.of(notStarted, inProgress, completed, partial, rejected));
    given(interviewRepository.findAllByHandoverPackage(handoverPackage))
        .willReturn(List.of(inProgressInterview, completedInterview, partialInterview, rejectedInterview));
    given(traceCardRepository.findAllByHandoverPackage(handoverPackage))
        .willReturn(List.of(card(completedInterview, CardStatus.DRAFT), card(rejectedInterview, CardStatus.REJECTED)));
    given(gapAnchorRepository.findAllByGapIn(any())).willReturn(List.of(anchor(inProgress)));

    List<GapResponse> responses = service.list(requesterOf(owner), handoverPackage.getId());

    assertThat(responses).extracting(GapResponse::progress).containsExactly(
        GapProgress.NOT_STARTED, GapProgress.IN_PROGRESS, GapProgress.COMPLETED, GapProgress.PARTIAL, GapProgress.REJECTED);
    assertThat(responses.get(1).anchors()).hasSize(1);
    assertThat(responses.get(0).anchors()).isEmpty();
    // Gap마다 쿼리하지 않고 Package 단위로 한 번씩만 읽는다
    verify(interviewRepository, times(1)).findAllByHandoverPackage(handoverPackage);
    verify(traceCardRepository, times(1)).findAllByHandoverPackage(handoverPackage);
    verify(gapAnchorRepository, times(1)).findAllByGapIn(any());
  }

  @Test
  void list_reviewerCanReadAndOtherPredecessorCannot() {
    given(gapRepository.findAllByHandoverPackageOrderByCreatedAtAsc(handoverPackage)).willReturn(List.of());
    given(gapAnchorRepository.findAllByGapIn(any())).willReturn(List.of());

    assertThat(service.list(requesterOf(member("reviewer", Position.REVIEWER)), handoverPackage.getId())).isEmpty();
    assertThat(service.list(requesterOf(owner), handoverPackage.getId())).isEmpty();

    Throwable otherPredecessor = catchThrowable(
        () -> service.list(requesterOf(member("other", Position.PREDECESSOR)), handoverPackage.getId()));
    Throwable successor = catchThrowable(
        () -> service.list(requesterOf(member("successor", Position.SUCCESSOR)), handoverPackage.getId()));

    assertThat(((CustomException) otherPredecessor).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    assertThat(((CustomException) successor).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void list_unknownPackage() {
    UUID unknownId = UUID.randomUUID();
    given(handoverPackageRepository.findById(unknownId)).willReturn(Optional.empty());

    Throwable thrown = catchThrowable(() -> service.list(requesterOf(owner), unknownId));

    assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  private Gap gap() {
    Gap gap = Gap.builder().handoverPackage(handoverPackage).type(GapType.CHOICE_GAP).summary("요약").question("질문").build();
    ReflectionTestUtils.setField(gap, "id", UUID.randomUUID());
    return gap;
  }

  private GapAnchor anchor(Gap gap) {
    return GapAnchor.builder().gap(gap).document(document).startOffset(0).endOffset(1).excerpt("x").build();
  }

  private Interview interview(Gap gap, InterviewStatus status, InterviewEndReason endReason) {
    Interview interview = Interview.builder().handoverPackage(handoverPackage).gap(gap).build();
    ReflectionTestUtils.setField(interview, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(interview, "status", status);
    ReflectionTestUtils.setField(interview, "endReason", endReason);
    return interview;
  }

  private TraceCard card(Interview interview, CardStatus status) {
    TraceCard card = TraceCard.builder().handoverPackage(handoverPackage).interview(interview).title("카드").build();
    ReflectionTestUtils.setField(card, "status", status);
    return card;
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
