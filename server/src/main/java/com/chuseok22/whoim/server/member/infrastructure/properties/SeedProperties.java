package com.chuseok22.whoim.server.member.infrastructure.properties;

import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 시연 계정 시드. seed.members가 없으면 빈 목록이다
@ConfigurationProperties(prefix = "seed")
public record SeedProperties(List<SeedMember> members) {

  public SeedProperties {
    members = members == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(members));
  }

  // record의 toString에 비밀번호가 나오므로 로그나 예외 메시지에 객체 전체를 넣지 않는다
  public record SeedMember(String username, String password, String name, Role role, Position position) {

  }
}
