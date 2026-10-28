package com.chuseok22.whoim.server.member.application.service;

import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.properties.SeedProperties.SeedMember;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

// 메시지에는 비밀번호가 새지 않도록 인덱스와 username만 담는다
public final class MemberSeedValidator {

  private static final Pattern EMPLOYEE_NUMBER_PATTERN = Pattern.compile("\\d{7}");

  private MemberSeedValidator() {
  }

  public static void validate(List<SeedMember> members) {
    Set<String> usernames = new HashSet<>();
    Set<String> employeeNumbers = new HashSet<>();
    for (int index = 0; index < members.size(); index++) {
      SeedMember member = members.get(index);
      if (member == null) {
        throw invalid(index, null, "항목이 비어 있습니다");
      }
      if (isBlank(member.username())) {
        throw invalid(index, null, "username이 비어 있습니다");
      }
      if (isBlank(member.password())) {
        throw invalid(index, member.username(), "password가 비어 있습니다");
      }
      if (isBlank(member.name())) {
        throw invalid(index, member.username(), "name이 비어 있습니다");
      }
      if (!usernames.add(member.username())) {
        throw invalid(index, member.username(), "username이 중복되었습니다");
      }
      validateRoleAndPosition(index, member);
      validateProfile(index, member, employeeNumbers);
    }
  }

  private static void validateProfile(int index, SeedMember member, Set<String> employeeNumbers) {
    if (member.role() == Role.ADMIN) {
      if (!isBlank(member.employeeNumber()) || !isBlank(member.department()) || !isBlank(member.jobTitle())) {
        throw invalid(index, member.username(), "ADMIN은 employeeNumber, department, jobTitle을 가질 수 없습니다");
      }
      return;
    }
    if (isBlank(member.employeeNumber())) {
      throw invalid(index, member.username(), "employeeNumber가 비어 있습니다");
    }
    if (isBlank(member.department())) {
      throw invalid(index, member.username(), "department가 비어 있습니다");
    }
    if (isBlank(member.jobTitle())) {
      throw invalid(index, member.username(), "jobTitle이 비어 있습니다");
    }
    if (!EMPLOYEE_NUMBER_PATTERN.matcher(member.employeeNumber()).matches()) {
      throw invalid(index, member.username(), "employeeNumber는 7자리 숫자여야 합니다");
    }
    if (!employeeNumbers.add(member.employeeNumber())) {
      throw invalid(index, member.username(), "employeeNumber가 중복되었습니다");
    }
  }

  private static void validateRoleAndPosition(int index, SeedMember member) {
    if (member.role() == null) {
      throw invalid(index, member.username(), "role이 비어 있습니다");
    }
    if (member.role() == Role.ADMIN && member.position() != null) {
      throw invalid(index, member.username(), "ADMIN은 position을 가질 수 없습니다");
    }
    if (member.role() == Role.USER && member.position() == null) {
      throw invalid(index, member.username(), "USER는 position이 필요합니다");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static IllegalStateException invalid(int index, String username, String reason) {
    String target = username == null ? "seed.members[" + index + "]" : "seed.members[" + index + "](" + username + ")";
    return new IllegalStateException(target + ": " + reason);
  }
}
