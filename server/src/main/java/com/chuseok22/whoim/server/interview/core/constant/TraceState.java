package com.chuseok22.whoim.server.interview.core.constant;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record TraceState(Map<TraceElement, TraceStateEntry> entries) {

  public TraceState {
    EnumMap<TraceElement, TraceStateEntry> copy = new EnumMap<>(TraceElement.class);
    copy.putAll(entries);
    entries = Collections.unmodifiableMap(copy);
  }

  public static TraceState empty() {
    return new TraceState(Map.of());
  }

  public TraceState merge(Map<TraceElement, TraceStateEntry> accepted) {
    EnumMap<TraceElement, TraceStateEntry> merged = new EnumMap<>(TraceElement.class);
    merged.putAll(entries);
    accepted.forEach((element, incoming) -> {
      TraceStateEntry current = merged.get(element);
      boolean keepsFilled = current != null
          && current.status() == TraceElementStatus.FILLED
          && incoming.status() == TraceElementStatus.UNKNOWN;
      if (!keepsFilled) {
        merged.put(element, incoming);
      }
    });
    return new TraceState(merged);
  }

  public boolean isChecklistComplete() {
    return missingElements().isEmpty();
  }

  public List<TraceElement> missingElements() {
    List<TraceElement> missing = new ArrayList<>();
    for (TraceElement element : TraceElement.values()) {
      if (!isFilled(element)) {
        missing.add(element);
      }
    }
    return missing;
  }

  public boolean hasFilledElement() {
    return entries.values().stream().anyMatch(entry -> entry.status() == TraceElementStatus.FILLED);
  }

  public boolean hasDirectFilledElement() {
    return entries.values().stream().anyMatch(entry ->
        entry.status() == TraceElementStatus.FILLED && entry.source() == EvidenceLinkType.DIRECT);
  }

  private boolean isFilled(TraceElement element) {
    TraceStateEntry entry = entries.get(element);
    return entry != null && entry.status() == TraceElementStatus.FILLED;
  }
}
