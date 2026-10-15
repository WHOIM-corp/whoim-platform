package com.chuseok22.whoim.server.auth.application.dto.data;

import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import java.util.UUID;

// position은 ADMIN이면 null이다
public record AuthenticatedMember(UUID memberId, String username, Role role, Position position) {

}
