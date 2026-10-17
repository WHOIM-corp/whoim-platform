package com.chuseok22.whoim.server.llm.infrastructure.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GeminiPropertiesTest {

  private static final String BASE_URL = "https://host/v1beta";
  private static final String API_KEY = "secret-value";
  private static final String MODEL = "model-name";

  @Test
  void blankOrNullValue_isRejected() {
    for (String invalid : new String[]{null, "", "   "}) {
      assertThatThrownBy(() -> new GeminiProperties(invalid, API_KEY, MODEL))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.gemini.base-url");
      assertThatThrownBy(() -> new GeminiProperties(BASE_URL, invalid, MODEL))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.gemini.api-key")
          .hasMessageNotContaining(API_KEY);
      assertThatThrownBy(() -> new GeminiProperties(BASE_URL, API_KEY, invalid))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("llm.gemini.model")
          .hasMessageNotContaining(API_KEY);
    }
  }

  @Test
  void trailingSlashInBaseUrl_isRemoved() {
    assertThat(new GeminiProperties("https://host/v1beta/", API_KEY, MODEL).baseUrl())
        .isEqualTo("https://host/v1beta");
  }

  @Test
  void validValues_pass() {
    assertThatCode(() -> new GeminiProperties(BASE_URL, API_KEY, MODEL)).doesNotThrowAnyException();
  }

  @Test
  void toString_doesNotExposeApiKey() {
    String text = new GeminiProperties(BASE_URL, API_KEY, MODEL).toString();

    assertThat(text).doesNotContain(API_KEY).contains(BASE_URL);
  }
}
