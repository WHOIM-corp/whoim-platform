package com.chuseok22.whoim.server.card.application.dto.data;

public record EvidenceCounts(int directCount, int recordedCount) {

  public static final EvidenceCounts ZERO = new EvidenceCounts(0, 0);
}
