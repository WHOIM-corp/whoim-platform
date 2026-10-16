package com.chuseok22.whoim.server.handover.core.constant;

import static com.chuseok22.whoim.server.handover.core.constant.HandoverStatus.PUBLISHED;
import static com.chuseok22.whoim.server.handover.core.constant.HandoverStatus.WRITING;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HandoverStatusTest {

  @Test
  void writingCanTransitionToPublished() {
    assertThat(WRITING.canTransitionTo(PUBLISHED)).isTrue();
  }

  @Test
  void writingCannotTransitionToItself() {
    assertThat(WRITING.canTransitionTo(WRITING)).isFalse();
  }

  @Test
  void publishedIsTerminal() {
    assertThat(PUBLISHED.canTransitionTo(WRITING)).isFalse();
    assertThat(PUBLISHED.canTransitionTo(PUBLISHED)).isFalse();
  }

  @Test
  void nullTargetIsRejected() {
    assertThat(WRITING.canTransitionTo(null)).isFalse();
    assertThat(PUBLISHED.canTransitionTo(null)).isFalse();
  }
}
