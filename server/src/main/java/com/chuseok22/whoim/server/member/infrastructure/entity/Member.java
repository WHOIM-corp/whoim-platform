package com.chuseok22.whoim.server.member.infrastructure.entity;

import com.chuseok22.whoim.server.common.infrastructure.persistence.BaseEntity;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  // ADMIN은 position이 없다. role-position 조합 검증은 시드 검증과 DB CHECK가 맡는다
  @Enumerated(EnumType.STRING)
  private Position position;

  @Builder
  private Member(String username, String password, String name, Role role, Position position) {
    this.username = username;
    this.password = password;
    this.name = name;
    this.role = role;
    this.position = position;
  }
}
