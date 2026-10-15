package com.chuseok22.whoim.server.member.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.properties.SeedProperties;
import com.chuseok22.whoim.server.member.infrastructure.properties.SeedProperties.SeedMember;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class MemberSeedRunnerTest {

  private static final String DUMMY_PASSWORD = "dummy-seed-pw-7f3a";

  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Test
  void emptyMembers_doesNotSave() {
    runnerWith(List.of()).run(null);

    verify(memberRepository, never()).save(any());
  }

  @Test
  void existingUsername_isNotSaved() {
    given(memberRepository.existsByUsername("existing")).willReturn(true);

    runnerWith(List.of(seed("existing", Role.USER, Position.REVIEWER))).run(null);

    verify(memberRepository, never()).save(any());
  }

  @Test
  void newUsername_isSavedWithEncodedPassword() {
    given(memberRepository.existsByUsername("fresh")).willReturn(false);

    runnerWith(List.of(seed("fresh", Role.USER, Position.SUCCESSOR))).run(null);

    ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
    verify(memberRepository).save(captor.capture());
    Member saved = captor.getValue();
    assertThat(saved.getUsername()).isEqualTo("fresh");
    assertThat(saved.getPassword()).isNotEqualTo(DUMMY_PASSWORD);
    assertThat(passwordEncoder.matches(DUMMY_PASSWORD, saved.getPassword())).isTrue();
  }

  @Test
  void invalidSeed_throwsIllegalStateAndDoesNotSave() {
    MemberSeedRunner runner = runnerWith(List.of(seed("admin", Role.ADMIN, Position.REVIEWER)));

    assertThatThrownBy(() -> runner.run(null)).isInstanceOf(IllegalStateException.class);
    verify(memberRepository, never()).save(any());
  }

  private MemberSeedRunner runnerWith(List<SeedMember> members) {
    return new MemberSeedRunner(new SeedProperties(members), memberRepository, passwordEncoder);
  }

  private SeedMember seed(String username, Role role, Position position) {
    return new SeedMember(username, DUMMY_PASSWORD, "이름", role, position);
  }
}
