package com.chuseok22.whoim.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.interview.core.constant.InterviewEndReason;
import com.chuseok22.whoim.server.interview.core.constant.InterviewStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

// 루트 테스트 패키지에 둔다: common이 다른 도메인을 import하지 않게 하기 위해서다.
// CHECK를 한 줄로 쓰고 CREATE TABLE을 ");" 한 줄로 닫는 SQL 스타일에 의존한다.
class SchemaEnumConsistencyTest {

  private static final Pattern QUOTED_VALUE = Pattern.compile("'([A-Z_]+)'");

  @Test
  void enumNamesMatchMigrationCheckConstraints() {
    assertMatches("handover_package", "status", HandoverStatus.class);
    assertMatches("evidence_document", "document_type", DocumentType.class);
    assertMatches("gap", "type", GapType.class);
    assertMatches("interview", "status", InterviewStatus.class);
    assertMatches("interview", "end_reason", InterviewEndReason.class);
    assertMatches("trace_card", "status", CardStatus.class);
    assertMatches("card_evidence_link", "type", EvidenceLinkType.class);
    assertMatches("card_state_history", "from_status", CardStatus.class);
    assertMatches("card_state_history", "to_status", CardStatus.class);
  }

  private void assertMatches(String table, String column, Class<? extends Enum<?>> enumType) {
    Set<String> enumNames = Arrays.stream(enumType.getEnumConstants())
        .map(Enum::name)
        .collect(Collectors.toSet());

    assertThat(checkValues(table, column))
        .as("%s.%s CHECK vs %s", table, column, enumType.getSimpleName())
        .isEqualTo(enumNames);
  }

  private Set<String> checkValues(String table, String column) {
    String sql = readAllMigrations();

    Matcher block = Pattern.compile("(?s)CREATE TABLE " + Pattern.quote(table) + "\\s*\\((.*?)\\n\\);")
        .matcher(sql);
    assertThat(block.find()).as("CREATE TABLE %s block not found", table).isTrue();

    Matcher check = Pattern.compile("\\b" + Pattern.quote(column) + "\\s+IN\\s*\\(([^)]*)\\)")
        .matcher(block.group(1));
    assertThat(check.find()).as("CHECK on %s.%s not found", table, column).isTrue();

    Set<String> values = new LinkedHashSet<>();
    Matcher value = QUOTED_VALUE.matcher(check.group(1));
    while (value.find()) {
      values.add(value.group(1));
    }
    assertThat(values).as("no quoted values in CHECK on %s.%s", table, column).isNotEmpty();
    return values;
  }

  private String readAllMigrations() {
    try {
      Resource[] resources = new PathMatchingResourcePatternResolver()
          .getResources("classpath:db/migration/*.sql");
      assertThat(resources).as("migration files on classpath").isNotEmpty();
      StringBuilder all = new StringBuilder();
      for (Resource resource : resources) {
        all.append(resource.getContentAsString(StandardCharsets.UTF_8)).append('\n');
      }
      return all.toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
