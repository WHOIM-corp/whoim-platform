package com.chuseok22.whoim.server.member.infrastructure.config;

import com.chuseok22.whoim.server.member.application.service.MemberSeedValidator;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.properties.SeedProperties;
import com.chuseok22.whoim.server.member.infrastructure.properties.SeedProperties.SeedMember;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@EnableConfigurationProperties(SeedProperties.class)
@RequiredArgsConstructor
public class MemberSeedRunner implements ApplicationRunner {

  private final SeedProperties seedProperties;
  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;

  // 이미 있는 username은 건드리지 않는다
  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (seedProperties.members().isEmpty()) {
      log.info("시드 계정 설정이 없어 건너뜁니다");
      return;
    }
    MemberSeedValidator.validate(seedProperties.members());
    int createdCount = 0;
    for (SeedMember seed : seedProperties.members()) {
      if (memberRepository.existsByUsername(seed.username())) {
        continue;
      }
      memberRepository.save(Member.builder()
          .username(seed.username())
          .password(passwordEncoder.encode(seed.password()))
          .name(seed.name())
          .role(seed.role())
          .position(seed.position())
          .build());
      createdCount++;
    }
    log.info("시드 계정 {}개 생성, {}개는 이미 존재", createdCount, seedProperties.members().size() - createdCount);
  }
}
