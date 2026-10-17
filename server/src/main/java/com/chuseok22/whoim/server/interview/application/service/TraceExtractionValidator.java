package com.chuseok22.whoim.server.interview.application.service;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.common.application.text.ExcerptMatcher;
import com.chuseok22.whoim.server.interview.application.dto.data.ExtractedElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// AI 추정을 FILLED로 저장하지 않도록 LLM 추출 결과를 원문과 대조한다
@Slf4j
@Component
public class TraceExtractionValidator {

  public Map<TraceElement, TraceStateEntry> accept(
      List<ExtractedElement> extracted, String answer, List<String> anchorExcerpts, int turnNumber) {
    Map<TraceElement, TraceStateEntry> accepted = new EnumMap<>(TraceElement.class);
    List<TraceElement> droppedKnownElements = new ArrayList<>();
    int droppedUnknownCount = 0;
    for (ExtractedElement item : extracted) {
      Optional<TraceElement> element = item == null
          ? Optional.empty() : parseEnum(TraceElement.class, item.element());
      if (element.isEmpty()) {
        droppedUnknownCount++;
        continue;
      }
      Optional<TraceStateEntry> entry = toEntry(item, answer, anchorExcerpts, turnNumber);
      if (entry.isPresent()) {
        accepted.put(element.get(), entry.get());
      } else {
        droppedKnownElements.add(element.get());
      }
    }
    if (!droppedKnownElements.isEmpty() || droppedUnknownCount > 0) {
      // LLM이 준 원문 문자열은 남기지 않는다
      log.warn("TRACE 추출 항목 제거: elements={}, invalidCount={}", droppedKnownElements, droppedUnknownCount);
    }
    return accepted;
  }

  private Optional<TraceStateEntry> toEntry(
      ExtractedElement item, String answer, List<String> anchorExcerpts, int turnNumber) {
    Optional<TraceElementStatus> status = parseEnum(TraceElementStatus.class, item.status());
    if (status.isEmpty()) {
      return Optional.empty();
    }
    if (status.get() == TraceElementStatus.UNKNOWN) {
      return Optional.of(new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, turnNumber));
    }
    Optional<EvidenceLinkType> source = parseEnum(EvidenceLinkType.class, item.source());
    if (source.isEmpty() || item.content() == null || item.content().isBlank()) {
      return Optional.empty();
    }
    Optional<String> matched = findExcerpt(source.get(), item.excerpt(), answer, anchorExcerpts);
    if (matched.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new TraceStateEntry(TraceElementStatus.FILLED, item.content(), source.get(), turnNumber));
  }

  private Optional<String> findExcerpt(
      EvidenceLinkType source, String excerpt, String answer, List<String> anchorExcerpts) {
    if (source == EvidenceLinkType.DIRECT) {
      return ExcerptMatcher.findIn(answer, excerpt);
    }
    return anchorExcerpts.stream()
        .map(anchorExcerpt -> ExcerptMatcher.findIn(anchorExcerpt, excerpt))
        .flatMap(Optional::stream)
        .findFirst();
  }

  // valueOf는 대소문자를 구분하므로 소문자·미지 이름은 거부된다
  private <E extends Enum<E>> Optional<E> parseEnum(Class<E> type, String name) {
    if (name == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(Enum.valueOf(type, name));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
