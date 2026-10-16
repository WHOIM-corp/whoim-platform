package com.chuseok22.whoim.server.llm.infrastructure.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SuhAiPropertiesTest {

  private static final String BASE_URL = "https://host/api/flask";
  private static final String API_KEY = "secret-value";
  private static final String MODEL = "model-name";

  @Test
  void blankOrNullValue_isRejected() {
    for (String invalid : new String[]{null, "", "   "}) {
      assertThatThrownBy(() -> new SuhAiProperties(invalid, API_KEY, MODEL))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.suh-ai.base-url");
      assertThatThrownBy(() -> new SuhAiProperties(BASE_URL, invalid, MODEL))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.suh-ai.api-key")
          .hasMessageNotContaining(API_KEY);
      assertThatThrownBy(() -> new SuhAiProperties(BASE_URL, API_KEY, invalid))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.suh-ai.model")
          .hasMessageNotContaining(API_KEY);
    }
  }

  @Test
  void trailingSlashInBaseUrl_isRemoved() {
    assertThat(new SuhAiProperties("https://host/api/flask/", API_KEY, MODEL).baseUrl())
        .isEqualTo("https://host/api/flask");
  }

  @Test
  void validValues_pass() {
    assertThatCode(() -> new SuhAiProperties(BASE_URL, API_KEY, MODEL)).doesNotThrowAnyException();
  }

  @Test
  void toString_doesNotExposeApiKey() {
    String text = new SuhAiProperties(BASE_URL, API_KEY, MODEL).toString();

    assertThat(text).doesNotContain(API_KEY).contains(BASE_URL);
  }
}
