package com.chuseok22.whoim.server.admin.application.service.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSectionResponse;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewRepository;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewTurnRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InterviewTableViewTest {

  @Mock
  private InterviewRepository interviewRepository;

  @Mock
  private InterviewTurnRepository interviewTurnRepository;

  @Mock
  private AdminPackageOptions adminPackageOptions;

  @InjectMocks
  private InterviewTableView interviewTableView;

  @Test
  void list_truncatesTraceStateAndLeavesNullEndReasonBlank() {
    Interview interview = interview();
    ReflectionTestUtils.setField(interview, "traceState", "t".repeat(100));
    given(interviewRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(interview)));

    AdminListResponse response = interviewTableView.list(0, null);

    assertThat(response.columns())
        .containsExactly("id", "handover_package_id", "gap_id", "status", "end_reason", "trace_state", "created_at");
    var cells = response.rows().get(0).cells();
    assertThat(cells.get(2).linkTableName()).isEqualTo("gap");
    assertThat(cells.get(4).empty()).isTrue();
    assertThat(cells.get(5).text()).hasSize(81).endsWith("…");
  }

  @Test
  void list_withPackageId_usesPackageQuery() {
    UUID packageId = UUID.randomUUID();
    given(interviewRepository.findAllByHandoverPackageId(eq(packageId), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of()));

    interviewTableView.list(0, packageId);

    verify(interviewRepository).findAllByHandoverPackageId(eq(packageId), any(Pageable.class));
  }

  @Test
  void detail_showsConversationSectionWithHeaderQuestionAnswerAndExtraction() {
    Interview interview = interview();
    InterviewTurn answered = turn(interview, 1, "질문1", "답변1", "{\"a\":1}");
    InterviewTurn unanswered = turn(interview, 2, "질문2", null, null);
    given(interviewRepository.findById(interview.getId())).willReturn(Optional.of(interview));
    given(interviewTurnRepository.findAllByInterviewOrderByTurnNumberAsc(interview))
        .willReturn(List.of(answered, unanswered));

    AdminDetailResponse response = interviewTableView.detail(interview.getId());

    assertThat(response.sections()).hasSize(1);
    AdminSectionResponse section = response.sections().get(0);
    assertThat(section.title()).isEqualTo("턴 (interview_turn)");
    assertThat(section.kind()).isEqualTo("CONVERSATION");
    var first = section.rows().get(0).cells();
    String shortId = answered.getId().toString().substring(0, 8);
    assertThat(first.get(0).text()).isEqualTo("TURN 1 · " + shortId + " · 2026-10-18 09:00:00");
    assertThat(first.get(1).text()).isEqualTo("질문1");
    assertThat(first.get(2).text()).isEqualTo("답변1");
    assertThat(first.get(3).text()).isEqualTo("{\"a\":1}");
    var second = section.rows().get(1).cells();
    assertThat(second.get(0).text()).startsWith("TURN 2 · ");
    assertThat(second.get(2).text()).isEqualTo("(답변 없음)");
    assertThat(second.get(3).empty()).isTrue();
  }

  private Interview interview() {
    HandoverPackage handoverPackage = HandoverPackage.builder().name("패키지").build();
    ReflectionTestUtils.setField(handoverPackage, "id", UUID.randomUUID());
    Gap gap = Gap.builder().handoverPackage(handoverPackage).type(GapType.values()[0])
        .summary("요약").question("질문").build();
    ReflectionTestUtils.setField(gap, "id", UUID.randomUUID());
    Interview interview = Interview.builder().handoverPackage(handoverPackage).gap(gap).build();
    ReflectionTestUtils.setField(interview, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(interview, "createdAt", Instant.parse("2026-10-18T00:00:00Z"));
    ReflectionTestUtils.setField(interview, "updatedAt", Instant.parse("2026-10-18T00:00:00Z"));
    return interview;
  }

  private InterviewTurn turn(Interview interview, int turnNumber, String question, String answer, String extraction) {
    InterviewTurn turn = InterviewTurn.builder().interview(interview).turnNumber(turnNumber).question(question).build();
    turn.recordAnswer(answer);
    turn.recordExtraction(extraction);
    ReflectionTestUtils.setField(turn, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(turn, "createdAt", Instant.parse("2026-10-18T00:00:00Z"));
    return turn;
  }
}
