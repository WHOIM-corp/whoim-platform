package com.chuseok22.whoim.server.card.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.setting.core.constant.DefaultPromptInstructions;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CardGenerationPromptTest {

  private final Gap gap = Gap.builder().type(GapType.CHOICE_GAP).summary("재시도 5회 근거 없음").question("왜 5회로 했나요?").build();

  @Test
  void build_containsGapFilledElementsAndDirectAnswersOnly() {
    TraceState state = new TraceState(Map.of(
        TraceElement.CAUSE, new TraceStateEntry(TraceElementStatus.FILLED, "트래픽 폭주", EvidenceLinkType.DIRECT, 1),
        TraceElement.RISK, new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, 2),
        TraceElement.ACTION, new TraceStateEntry(TraceElementStatus.FILLED, "기록의 재시도 설정", EvidenceLinkType.RECORDED, 2)));
    InterviewTurn directTurn = turn(1, "트래픽이 몰려서 5회로 정했습니다");
    InterviewTurn recordedOnlyTurn = turn(2, "문서에 있는 그대로입니다");

    LlmRequest request = CardGenerationPrompt.build(DefaultPromptInstructions.CARD_GENERATION, gap, state, List.of(directTurn, recordedOnlyTurn));

    assertThat(request.prompt()).contains("재시도 5회 근거 없음", "왜 5회로 했나요?", "CAUSE: 트래픽 폭주", "ACTION: 기록의 재시도 설정");
    assertThat(request.prompt()).doesNotContain("RISK:");
    assertThat(request.prompt()).contains("턴 1: 트래픽이 몰려서 5회로 정했습니다");
    assertThat(request.prompt()).doesNotContain("문서에 있는 그대로입니다");
  }

  @Test
  void build_systemForbidsNewFactsAndRequiresVerbatimCopy() {
    LlmRequest request = CardGenerationPrompt.build(DefaultPromptInstructions.CARD_GENERATION, gap, TraceState.empty(), List.of());

    assertThat(request.system()).contains("입력에 없는", "title은", "원문을 그대로", "directEvidence", "null", "JSON만");
  }

  @Test
  void build_appendsFormatBlock_toDefaultInstruction() {
    LlmRequest request = CardGenerationPrompt.build(
        DefaultPromptInstructions.CARD_GENERATION, gap, TraceState.empty(), List.of());

    assertThat(request.system()).startsWith(DefaultPromptInstructions.CARD_GENERATION);
    assertThat(request.system()).contains("응답 형태:", "{\"title\":\"특정 조건에서", "\"directEvidence\"",
        "directEvidence에는 유효한 항목을 최소 1개 포함합니다");
  }

  @Test
  void build_wrapsSectionsInSentinel() {
    TraceState state = new TraceState(Map.of(
        TraceElement.CAUSE, new TraceStateEntry(TraceElementStatus.FILLED, "트래픽 폭주", EvidenceLinkType.DIRECT, 1)));

    String prompt = CardGenerationPrompt.build(
        DefaultPromptInstructions.CARD_GENERATION, gap, state, List.of(turn(1, "답변"))).prompt();

    assertThat(prompt).startsWith("다음 인터뷰 결과로 TRACE Card를 만드세요.\n\n===== BEGIN INPUT DATA =====\n\n[Gap 요약]");
    assertThat(prompt).endsWith("[답변 발췌 후보]\n- 턴 1: 답변\n\n===== END INPUT DATA =====");
  }

  @Test
  void build_appendsFormatBlock_toCustomInstruction() {
    LlmRequest request = CardGenerationPrompt.build("커스텀", gap, TraceState.empty(), List.of());

    assertThat(request.system()).startsWith("커스텀\n\n").contains("JSON만 출력", "응답 형태:", "\"directEvidence\"");
  }

  @Test
  void build_appendsFormatBlock_whenInstructionIsEmptyString() {
    LlmRequest request = CardGenerationPrompt.build("", gap, TraceState.empty(), List.of());

    assertThat(request.system()).contains("JSON만 출력", "응답 형태:", "\"directEvidence\"");
  }

  @Test
  void build_userPromptIsIndependentOfInstruction() {
    assertThat(CardGenerationPrompt.build("커스텀", gap, TraceState.empty(), List.of()).prompt())
        .isEqualTo(CardGenerationPrompt.build(
            DefaultPromptInstructions.CARD_GENERATION, gap, TraceState.empty(), List.of()).prompt());
  }

  private static InterviewTurn turn(int number, String answer) {
    InterviewTurn turn = InterviewTurn.builder().turnNumber(number).question("질문").build();
    turn.recordAnswer(answer);
    return turn;
  }
}
