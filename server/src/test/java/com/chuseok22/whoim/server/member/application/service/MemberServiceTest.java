package com.chuseok22.whoim.server.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.member.application.dto.response.MemberSearchResponse;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

  @Mock
  private MemberRepository memberRepository;
  @InjectMocks
  private MemberService service;

  @Test
  void search_mapsMembersToResponseWithoutUsername() {
    Member first = givenMember("가", "A-001", "개발팀", "선임");
    Member second = givenMember("나", "A-002", "운영팀", "책임");
    given(memberRepository.searchByPosition(eq(Position.SUCCESSOR), any(), any(Pageable.class)))
        .willReturn(List.of(first, second));

    List<MemberSearchResponse> responses = service.search(Position.SUCCESSOR, "가", 6);

    assertThat(responses).extracting(MemberSearchResponse::memberId).containsExactly(first.getId(), second.getId());
    assertThat(responses).extracting(MemberSearchResponse::employeeNumber).containsExactly("A-001", "A-002");
    assertThat(responses).extracting(MemberSearchResponse::department).containsExactly("개발팀", "운영팀");
    assertThat(responses).extracting(MemberSearchResponse::jobTitle).containsExactly("선임", "책임");
    assertThat(responses).extracting(MemberSearchResponse::position).containsOnly(Position.SUCCESSOR);
  }

  @Test
  void search_blankKeyword_matchesAll() {
    for (String keyword : new String[] {null, "", "  "}) {
      assertThat(capturePattern(keyword, 6)).isEqualTo("%");
    }
  }

  @Test
  void search_escapesLikeWildcards() {
    assertThat(capturePattern("50%_A\\", 6)).isEqualTo("%50\\%\\_a\\\\%");
  }

  @Test
  void search_clampsLimit() {
    assertThat(capturePageSize(0)).isEqualTo(1);
    assertThat(capturePageSize(999)).isEqualTo(20);
    assertThat(capturePageSize(6)).isEqualTo(6);
  }

  private String capturePattern(String keyword, int limit) {
    ArgumentCaptor<String> pattern = ArgumentCaptor.forClass(String.class);
    service.search(Position.SUCCESSOR, keyword, limit);
    verify(memberRepository, org.mockito.Mockito.atLeastOnce())
        .searchByPosition(eq(Position.SUCCESSOR), pattern.capture(), any(Pageable.class));
    return pattern.getValue();
  }

  private int capturePageSize(int limit) {
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    service.search(Position.SUCCESSOR, "a", limit);
    verify(memberRepository, org.mockito.Mockito.atLeastOnce())
        .searchByPosition(eq(Position.SUCCESSOR), any(), pageable.capture());
    return pageable.getValue().getPageSize();
  }

  private static Member givenMember(String name, String employeeNumber, String department, String jobTitle) {
    Member member = Member.builder().username(name).name(name).role(Role.USER).position(Position.SUCCESSOR)
        .employeeNumber(employeeNumber).department(department).jobTitle(jobTitle).build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }
}
