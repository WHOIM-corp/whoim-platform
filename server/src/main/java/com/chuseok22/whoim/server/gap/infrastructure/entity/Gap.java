package com.chuseok22.whoim.server.gap.infrastructure.entity;

import com.chuseok22.whoim.server.common.infrastructure.persistence.BaseEntity;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gap extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private HandoverPackage handoverPackage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GapType type;

  @Column(nullable = false)
  private String summary;

  @Column(nullable = false)
  private String question;

  @Builder
  private Gap(HandoverPackage handoverPackage, GapType type, String summary, String question) {
    this.handoverPackage = handoverPackage;
    this.type = type;
    this.summary = summary;
    this.question = question;
  }
}
