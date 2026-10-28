package com.chuseok22.whoim.server.admin.application.service.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminCell;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberTableViewTest {

  private static final String PASSWORD_HASH = "{bcrypt}secret-hash";

  @Mock
  private MemberRepository memberRepository;

  @InjectMocks
  private MemberTableView memberTableView;

  @Test
  void list_neverExposesPassword() {
    Member predecessor = member("predecessor-1", Role.USER, Position.PREDECESSOR);
    Member admin = member("admin-1", Role.ADMIN, null);
    given(memberRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(predecessor, admin)));

    AdminListResponse response = memberTableView.list(0, null);

    assertThat(response.columns()).doesNotContain("password");
    assertThat(response.columns()).containsExactly("id", "username", "name", "role", "position",
        "employee_number", "department", "job_title", "created_at");
    assertThat(response.rows()).hasSize(2);
    assertThat(response.rows()).allSatisfy(row -> {
      assertThat(row.cells()).hasSameSizeAs(response.columns());
      assertThat(row.cells()).noneMatch(cell -> PASSWORD_HASH.equals(cell.text()));
    });
  }

  @Test
  void list_rendersNullPositionAsEmptyCell() {
    Member admin = member("admin-1", Role.ADMIN, null);
    given(memberRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(admin)));

    AdminListResponse response = memberTableView.list(0, null);

    AdminCell positionCell = response.rows().get(0).cells().get(4);
    assertThat(positionCell.empty()).isTrue();
  }

  @Test
  void list_rendersProfileCells() {
    Member predecessor = member("predecessor-1", Role.USER, Position.PREDECESSOR);
    ReflectionTestUtils.setField(predecessor, "employeeNumber", "20260001");
    ReflectionTestUtils.setField(predecessor, "department", "개발팀");
    ReflectionTestUtils.setField(predecessor, "jobTitle", "선임");
    given(memberRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(predecessor)));

    AdminListResponse response = memberTableView.list(0, null);

    assertThat(response.rows().get(0).cells().subList(5, 8)).extracting(AdminCell::text)
        .containsExactly("20260001", "개발팀", "선임");
  }

  @Test
  void list_adminWithoutProfile_rendersEmptyCells() {
    Member admin = member("admin-1", Role.ADMIN, null);
    given(memberRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(admin)));

    AdminListResponse response = memberTableView.list(0, null);

    assertThat(response.rows().get(0).cells().subList(5, 8)).allSatisfy(cell -> assertThat(cell.empty()).isTrue());
  }

  @Test
  void detail_containsProfileFields() {
    Member predecessor = member("predecessor-1", Role.USER, Position.PREDECESSOR);
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));

    AdminDetailResponse response = memberTableView.detail(predecessor.getId());

    assertThat(response.fields()).extracting(field -> field.label())
        .contains("employee_number", "department", "job_title");
  }

  @Test
  void detail_doesNotContainPassword() {
    Member predecessor = member("predecessor-1", Role.USER, Position.PREDECESSOR);
    UUID id = predecessor.getId();
    given(memberRepository.findById(id)).willReturn(Optional.of(predecessor));

    AdminDetailResponse response = memberTableView.detail(id);

    assertThat(response.fields()).noneMatch(field -> "password".equals(field.label()));
    assertThat(response.fields()).noneMatch(field -> PASSWORD_HASH.equals(field.value().text()));
  }

  @Test
  void detail_unknownId_throwsNotFound() {
    UUID unknownId = UUID.randomUUID();
    given(memberRepository.findById(unknownId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> memberTableView.detail(unknownId))
        .isInstanceOfSatisfying(CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private Member member(String username, Role role, Position position) {
    Member member = Member.builder()
        .username(username)
        .password(PASSWORD_HASH)
        .name("테스트 " + username)
        .role(role)
        .position(position)
        .build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(member, "createdAt", Instant.parse("2026-10-18T00:00:00Z"));
    ReflectionTestUtils.setField(member, "updatedAt", Instant.parse("2026-10-18T00:00:00Z"));
    return member;
  }
}
