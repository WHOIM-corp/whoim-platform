package com.chuseok22.whoim.server.member.application.dto.response;

import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import java.util.UUID;

public record MemberResponse(UUID memberId, String username, String name, Role role, Position position) {

  public static MemberResponse from(Member member) {
    return new MemberResponse(
        member.getId(), member.getUsername(), member.getName(), member.getRole(), member.getPosition());
  }
}
