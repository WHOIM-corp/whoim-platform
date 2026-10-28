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
    assertFails(new SeedMember(" ", DUMMY_PASSWORD, "이름", Role.USER, Position.REVIEWER, "2026001", "팀", "사원"));
  }

  @Test
  void blankPassword_fails() {
    assertFails(new SeedMember("user", "", "이름", Role.USER, Position.REVIEWER, "2026001", "팀", "사원"));
  }

  @Test
  void blankName_fails() {
    assertFails(new SeedMember("user", DUMMY_PASSWORD, null, Role.USER, Position.REVIEWER, "2026001", "팀", "사원"));
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
    assertFails(new SeedMember("user", DUMMY_PASSWORD, "이름", null, Position.REVIEWER, "2026001", "팀", "사원"));
  }

  @Test
  void user_withoutEmployeeNumber_throws() {
    assertMessageContains(user(null, "팀", "사원"), "seed.members[0](user)", "employeeNumber");
  }

  @Test
  void user_withBlankDepartment_throws() {
    assertMessageContains(user("2026001", " ", "사원"), "seed.members[0](user)", "department");
  }

  @Test
  void user_withBlankJobTitle_throws() {
    assertMessageContains(user("2026001", "팀", ""), "seed.members[0](user)", "jobTitle");
  }

  @Test
  void admin_withProfile_throws() {
    assertFails(new SeedMember("admin", DUMMY_PASSWORD, "이름", Role.ADMIN, null, "2026001", "팀", "사원"));
  }

  @Test
  void employeeNumber_notSevenDigits_throws() {
    for (String invalid : List.of("202601", "20260011", "2026-01", "ABC1234")) {
      assertFails(user(invalid, "팀", "사원"));
    }
  }

  @Test
  void employeeNumber_duplicate_throws() {
    assertFails(
        new SeedMember("a", DUMMY_PASSWORD, "이름", Role.USER, Position.REVIEWER, "2026001", "팀", "사원"),
        new SeedMember("b", DUMMY_PASSWORD, "이름", Role.USER, Position.SUCCESSOR, "2026001", "팀", "사원"));
  }

  @Test
  void validUserAndAdmin_passes() {
    List<SeedMember> members = List.of(
        new SeedMember("a", DUMMY_PASSWORD, "이름", Role.USER, Position.PREDECESSOR, "2012001", "결제플랫폼팀", "부장"),
        new SeedMember("b", DUMMY_PASSWORD, "이름", Role.USER, Position.REVIEWER, "2010001", "서비스기획팀", "부장"),
        new SeedMember("c", DUMMY_PASSWORD, "이름", Role.USER, Position.SUCCESSOR, "2026001", "결제플랫폼팀", "사원"),
        new SeedMember("d", DUMMY_PASSWORD, "이름", Role.ADMIN, null, null, null, null));

    assertThatCode(() -> MemberSeedValidator.validate(members)).doesNotThrowAnyException();
  }

  private void assertMessageContains(SeedMember member, String... fragments) {
    assertThatThrownBy(() -> MemberSeedValidator.validate(List.of(member)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageNotContaining(DUMMY_PASSWORD)
        .hasMessageContainingAll(fragments);
  }

  private void assertFails(SeedMember... members) {
    assertThatThrownBy(() -> MemberSeedValidator.validate(List.of(members)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageNotContaining(DUMMY_PASSWORD);
  }

  private SeedMember member(String username, Role role, Position position) {
    return role == Role.ADMIN
        ? new SeedMember(username, DUMMY_PASSWORD, "이름", role, position, null, null, null)
        : new SeedMember(username, DUMMY_PASSWORD, "이름", role, position, employeeNumberOf(username), "팀", "사원");
  }

  // username마다 다른 7자리 사번을 만들어 사번 중복 검사에 걸리지 않게 한다
  private String employeeNumberOf(String username) {
    return String.format("%07d", Math.abs(username.hashCode()) % 10_000_000);
  }

  private SeedMember user(String employeeNumber, String department, String jobTitle) {
    return new SeedMember("user", DUMMY_PASSWORD, "이름", Role.USER, Position.REVIEWER, employeeNumber, department, jobTitle);
  }
}
