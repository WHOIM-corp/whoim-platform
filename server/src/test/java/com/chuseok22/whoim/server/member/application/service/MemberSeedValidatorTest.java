package com.chuseok22.whoim.server.member.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.properties.SeedProperties.SeedMember;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemberSeedValidatorTest {

  private static final String DUMMY_PASSWORD = "dummy-seed-pw-7f3a";

  @Test
  void validFourAccounts_pass() {
    List<SeedMember> members = List.of(
        member("predecessor", Role.USER, Position.PREDECESSOR),
        member("successor", Role.USER, Position.SUCCESSOR),
        member("reviewer", Role.USER, Position.REVIEWER),
        member("admin", Role.ADMIN, null));

    assertThatCode(() -> MemberSeedValidator.validate(members)).doesNotThrowAnyException();
  }

  @Test
  void emptyList_passes() {
    assertThatCode(() -> MemberSeedValidator.validate(List.of())).doesNotThrowAnyException();
  }

  @Test
  void blankUsername_fails() {
    assertFails(new SeedMember(" ", DUMMY_PASSWORD, "이름", Role.USER, Position.REVIEWER));
  }

  @Test
  void blankPassword_fails() {
    assertFails(new SeedMember("user", "", "이름", Role.USER, Position.REVIEWER));
  }

  @Test
  void blankName_fails() {
    assertFails(new SeedMember("user", DUMMY_PASSWORD, null, Role.USER, Position.REVIEWER));
  }

  @Test
  void duplicateUsername_fails() {
    assertFails(member("same", Role.USER, Position.REVIEWER), member("same", Role.USER, Position.SUCCESSOR));
  }

  @Test
  void adminWithPosition_fails() {
    assertFails(member("admin", Role.ADMIN, Position.REVIEWER));
  }

  @Test
  void userWithoutPosition_fails() {
    assertFails(member("user", Role.USER, null));
  }

  @Test
  void missingRole_fails() {
    assertFails(new SeedMember("user", DUMMY_PASSWORD, "이름", null, Position.REVIEWER));
  }

  private void assertFails(SeedMember... members) {
    assertThatThrownBy(() -> MemberSeedValidator.validate(List.of(members)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageNotContaining(DUMMY_PASSWORD);
  }

  private SeedMember member(String username, Role role, Position position) {
    return new SeedMember(username, DUMMY_PASSWORD, "이름", role, position);
  }
}
