package com.chuseok22.whoim.server.admin.core.constant;

import java.util.Arrays;
import java.util.Optional;

public enum AdminTable {
  MEMBER("member", "계정 (비밀번호는 표시하지 않음)", "-", false),
  HANDOVER_PACKAGE("handover_package", "인수인계 Package", "-", false),
  EVIDENCE_DOCUMENT("evidence_document", "업로드 문서", "-", true),
  GAP("gap", "탐지된 Gap", "근거 앵커 (gap_anchor)", true),
  INTERVIEW("interview", "Phronesis 인터뷰", "턴 (interview_turn)", true),
  TRACE_CARD("trace_card", "TRACE Card", "근거 링크 (card_evidence_link), 상태 이력 (card_state_history)", true);

  private final String pathName;
  private final String description;
  private final String childrenNote;
  private final boolean packageFilterable;

  AdminTable(String pathName, String description, String childrenNote, boolean packageFilterable) {
    this.pathName = pathName;
    this.description = description;
    this.childrenNote = childrenNote;
    this.packageFilterable = packageFilterable;
  }

  public String pathName() {
    return pathName;
  }

  public String description() {
    return description;
  }

  public String childrenNote() {
    return childrenNote;
  }

  public boolean packageFilterable() {
    return packageFilterable;
  }

  public static Optional<AdminTable> fromPathName(String pathName) {
    return Arrays.stream(values())
        .filter(table -> table.pathName.equals(pathName))
        .findFirst();
  }
}
