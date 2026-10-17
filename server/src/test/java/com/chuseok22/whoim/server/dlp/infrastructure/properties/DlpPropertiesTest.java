package com.chuseok22.whoim.server.dlp.infrastructure.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chuseok22.whoim.server.dlp.core.constant.DlpEntityType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DlpPropertiesTest {

  @Test
  void constructor_rejectsKeysOutsideDictionaryTypes() {
    assertThatThrownBy(() -> new DlpProperties(Map.of(DlpEntityType.EMAIL, List.of("a"))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void constructor_acceptsDictionaryTypes() {
    DlpProperties properties = new DlpProperties(Map.of(DlpEntityType.CUSTOMER, List.of("가나다상사")));

    assertThat(properties.entities().get(DlpEntityType.CUSTOMER)).containsExactly("가나다상사");
  }

  @Test
  void constructor_isNullSafe() {
    assertThat(new DlpProperties(null).entities()).isEmpty();

    Map<DlpEntityType, List<String>> withNullList = new java.util.HashMap<>();
    withNullList.put(DlpEntityType.PERSON, null);
    assertThat(new DlpProperties(withNullList).entities().get(DlpEntityType.PERSON)).isEmpty();
  }
}
