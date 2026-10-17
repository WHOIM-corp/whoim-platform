package com.chuseok22.whoim.server.dlp.infrastructure.properties;

import com.chuseok22.whoim.server.dlp.core.constant.DlpEntityType;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 사전 용어는 사용자가 채우는 값이라 toString을 재정의해 로그로 새지 않게 한다
@ConfigurationProperties(prefix = "dlp")
public record DlpProperties(Map<DlpEntityType, List<String>> entities) {

  private static final Set<DlpEntityType> DICTIONARY_TYPES =
      EnumSet.of(DlpEntityType.CUSTOMER, DlpEntityType.PARTNER, DlpEntityType.PROJECT, DlpEntityType.PERSON);

  public DlpProperties {
    Map<DlpEntityType, List<String>> normalized = new EnumMap<>(DlpEntityType.class);
    if (entities != null) {
      entities.forEach((type, terms) -> {
        if (!DICTIONARY_TYPES.contains(type)) {
          throw new IllegalStateException("dlp.entities에 허용되지 않는 키입니다: " + type);
        }
        normalized.put(type, terms == null ? List.of() : terms.stream().filter(Objects::nonNull).toList());
      });
    }
    entities = Map.copyOf(normalized);
  }

  @Override
  public String toString() {
    return "DlpProperties[types=" + entities.keySet() + "]";
  }
}
