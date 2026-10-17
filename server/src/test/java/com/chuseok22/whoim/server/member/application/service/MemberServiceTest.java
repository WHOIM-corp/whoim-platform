package com.chuseok22.whoim.server.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.chuseok22.whoim.server.member.application.dto.response.MemberResponse;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

  @Mock
  private MemberRepository memberRepository;
  @InjectMocks
  private MemberService service;

  @Test
  void listByPosition_mapsMembersOfPosition() {
    Member first = givenMember("가", Position.SUCCESSOR);
    Member second = givenMember("나", Position.SUCCESSOR);
    given(memberRepository.findAllByPositionOrderByNameAsc(Position.SUCCESSOR)).willReturn(List.of(first, second));

    List<MemberResponse> responses = service.listByPosition(Position.SUCCESSOR);

    assertThat(responses).extracting(MemberResponse::memberId).containsExactly(first.getId(), second.getId());
    assertThat(responses).extracting(MemberResponse::position).containsOnly(Position.SUCCESSOR);
  }

  private static Member givenMember(String name, Position position) {
    Member member = Member.builder().username(name).name(name).role(Role.USER).position(position).build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }
}
