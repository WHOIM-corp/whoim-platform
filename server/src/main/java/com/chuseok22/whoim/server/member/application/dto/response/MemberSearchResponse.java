package com.chuseok22.whoim.server.member.application.dto.response;

import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import java.util.UUID;

public record MemberSearchResponse(
    UUID memberId, String name, String employeeNumber, String department, String jobTitle, Position position) {

  public static MemberSearchResponse from(Member member) {
    return new MemberSearchResponse(member.getId(), member.getName(), member.getEmployeeNumber(),
        member.getDepartment(), member.getJobTitle(), member.getPosition());
  }
}
