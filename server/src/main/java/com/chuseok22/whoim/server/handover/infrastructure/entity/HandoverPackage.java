package com.chuseok22.whoim.server.handover.infrastructure.entity;

import com.chuseok22.whoim.server.common.infrastructure.persistence.BaseEntity;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HandoverPackage extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Member predecessor;

  @ManyToOne(fetch = FetchType.LAZY)
  private Member successor;

  @Column(nullable = false)
  private String name;

  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private HandoverStatus status;

  private Instant publishedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  private Member publishedBy;

  @Builder
  private HandoverPackage(Member predecessor, Member successor, String name, String description) {
    this.predecessor = predecessor;
    this.successor = successor;
    this.name = name;
    this.description = description;
    this.status = HandoverStatus.WRITING;
  }
}
