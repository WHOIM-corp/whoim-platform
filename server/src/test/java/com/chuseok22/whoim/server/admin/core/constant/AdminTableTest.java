package com.chuseok22.whoim.server.admin.core.constant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AdminTableTest {

  @Test
  void fromPathName_findsRegisteredTable() {
    assertThat(AdminTable.fromPathName("member")).contains(AdminTable.MEMBER);
  }

  @Test
  void fromPathName_rejectsUnregisteredNames() {
    assertThat(AdminTable.fromPathName("setting_config")).isEmpty();
    assertThat(AdminTable.fromPathName("gap_anchor")).isEmpty();
    assertThat(AdminTable.fromPathName("MEMBER")).isEmpty();
  }

  @Test
  void packageFilterable_isExactlyFourTables() {
    Set<AdminTable> filterable = Arrays.stream(AdminTable.values())
        .filter(AdminTable::packageFilterable)
        .collect(Collectors.toSet());

    assertThat(filterable).containsExactlyInAnyOrder(
        AdminTable.EVIDENCE_DOCUMENT, AdminTable.GAP, AdminTable.INTERVIEW, AdminTable.TRACE_CARD);
  }
}
