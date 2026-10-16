package com.chuseok22.whoim.server.handover.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageCreateRequest;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageSuccessorRequest;
import com.chuseok22.whoim.server.handover.application.dto.response.HandoverPackageResponse;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HandoverPackageServiceTest {

  @Mock
  private HandoverPackageRepository handoverPackageRepository;
  @Mock
  private MemberRepository memberRepository;

  private HandoverPackageService service;

  private Member predecessor;
  private Member successor;
  private Member reviewer;

  @BeforeEach
  void setUp() {
    service = new HandoverPackageService(handoverPackageRepository, memberRepository);
    predecessor = givenMember("predecessor", Position.PREDECESSOR);
    successor = givenMember("successor", Position.SUCCESSOR);
    reviewer = givenMember("reviewer", Position.REVIEWER);
  }

  @Test
  void create_savesWritingPackage() {
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));
    given(memberRepository.findById(successor.getId())).willReturn(Optional.of(successor));
    given(handoverPackageRepository.save(any(HandoverPackage.class))).willAnswer(returnsFirstArg());

    HandoverPackageResponse response = service.create(
        new HandoverPackageCreateRequest(predecessor.getId(), successor.getId(), "인수인계", "설명"));

    ArgumentCaptor<HandoverPackage> captor = ArgumentCaptor.forClass(HandoverPackage.class);
    verify(handoverPackageRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(HandoverStatus.WRITING);
    assertThat(captor.getValue().getSuccessor()).isSameAs(successor);
    assertThat(response.name()).isEqualTo("인수인계");
    assertThat(response.description()).isEqualTo("설명");
    assertThat(response.status()).isEqualTo(HandoverStatus.WRITING);
    assertThat(response.predecessor().memberId()).isEqualTo(predecessor.getId());
    assertThat(response.successor().memberId()).isEqualTo(successor.getId());
  }

  @Test
  void create_withoutSuccessor_savesNullSuccessor() {
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));
    given(handoverPackageRepository.save(any(HandoverPackage.class))).willAnswer(returnsFirstArg());

    HandoverPackageResponse response = service.create(
        new HandoverPackageCreateRequest(predecessor.getId(), null, "인수인계", null));

    assertThat(response.successor()).isNull();
    assertThat(response.description()).isNull();
  }

  @Test
  void create_rejectsPredecessorWithWrongPosition() {
    given(memberRepository.findById(successor.getId())).willReturn(Optional.of(successor));

    assertThat(errorCodeOf(() -> service.create(
        new HandoverPackageCreateRequest(successor.getId(), null, "인수인계", null))))
        .isEqualTo(ErrorCode.INVALID_REQUEST);
  }

  @Test
  void create_rejectsSuccessorWithWrongPosition() {
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));
    given(memberRepository.findById(reviewer.getId())).willReturn(Optional.of(reviewer));

    assertThat(errorCodeOf(() -> service.create(
        new HandoverPackageCreateRequest(predecessor.getId(), reviewer.getId(), "인수인계", null))))
        .isEqualTo(ErrorCode.INVALID_REQUEST);
  }

  @Test
  void create_rejectsDuplicateName() {
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));
    given(handoverPackageRepository.existsByPredecessorAndName(predecessor, "인수인계")).willReturn(true);

    assertThat(errorCodeOf(() -> service.create(
        new HandoverPackageCreateRequest(predecessor.getId(), null, "인수인계", null))))
        .isEqualTo(ErrorCode.STATE_CONFLICT);
  }

  @Test
  void create_unknownPredecessor() {
    UUID unknownId = UUID.randomUUID();
    given(memberRepository.findById(unknownId)).willReturn(Optional.empty());

    assertThat(errorCodeOf(() -> service.create(
        new HandoverPackageCreateRequest(unknownId, null, "인수인계", null))))
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void updateSuccessor_assignsAndClears() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    given(handoverPackageRepository.findWithLockById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(memberRepository.findById(successor.getId())).willReturn(Optional.of(successor));

    HandoverPackageResponse assigned = service.updateSuccessor(
        handoverPackage.getId(), new HandoverPackageSuccessorRequest(successor.getId()));
    assertThat(assigned.successor().memberId()).isEqualTo(successor.getId());

    HandoverPackageResponse cleared = service.updateSuccessor(
        handoverPackage.getId(), new HandoverPackageSuccessorRequest(null));
    assertThat(cleared.successor()).isNull();
  }

  @Test
  void updateSuccessor_rejectsWrongPosition() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    given(handoverPackageRepository.findWithLockById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(memberRepository.findById(reviewer.getId())).willReturn(Optional.of(reviewer));

    assertThat(errorCodeOf(() -> service.updateSuccessor(
        handoverPackage.getId(), new HandoverPackageSuccessorRequest(reviewer.getId()))))
        .isEqualTo(ErrorCode.INVALID_REQUEST);
  }

  @Test
  void updateSuccessor_rejectsPublished() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);
    given(handoverPackageRepository.findWithLockById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));

    assertThat(errorCodeOf(() -> service.updateSuccessor(
        handoverPackage.getId(), new HandoverPackageSuccessorRequest(null))))
        .isEqualTo(ErrorCode.STATE_CONFLICT);
  }

  @Test
  void updateSuccessor_unknownPackage() {
    UUID unknownId = UUID.randomUUID();
    given(handoverPackageRepository.findWithLockById(unknownId)).willReturn(Optional.empty());

    assertThat(errorCodeOf(() -> service.updateSuccessor(unknownId, new HandoverPackageSuccessorRequest(null))))
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void list_reviewerSeesAll_predecessorSeesOwnOnly() {
    HandoverPackage owned = givenPackage(predecessor);
    HandoverPackage other = givenPackage(givenMember("other", Position.PREDECESSOR));
    given(handoverPackageRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(owned, other));
    given(handoverPackageRepository.findAllByPredecessorOrderByCreatedAtDesc(predecessor)).willReturn(List.of(owned));
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));

    assertThat(service.list(requesterOf(reviewer))).hasSize(2);
    assertThat(service.list(requesterOf(predecessor)))
        .extracting(HandoverPackageResponse::id).containsExactly(owned.getId());
  }

  @Test
  void list_rejectsOtherPositions() {
    assertThat(errorCodeOf(() -> service.list(requesterOf(successor)))).isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void get_allowsReviewerAndOwnerOnly() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));

    assertThat(service.get(requesterOf(reviewer), handoverPackage.getId()).id()).isEqualTo(handoverPackage.getId());
    assertThat(service.get(requesterOf(predecessor), handoverPackage.getId()).id()).isEqualTo(handoverPackage.getId());
  }

  @Test
  void get_rejectsOtherPredecessor() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    AuthenticatedMember otherPredecessor = requesterOf(givenMember("other", Position.PREDECESSOR));

    assertThat(errorCodeOf(() -> service.get(otherPredecessor, handoverPackage.getId())))
        .isEqualTo(ErrorCode.ACCESS_DENIED);
    assertThat(errorCodeOf(() -> service.get(requesterOf(successor), handoverPackage.getId())))
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void get_unknownId() {
    UUID unknownId = UUID.randomUUID();
    given(handoverPackageRepository.findById(unknownId)).willReturn(Optional.empty());

    assertThat(errorCodeOf(() -> service.get(requesterOf(reviewer), unknownId)))
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  private static ErrorCode errorCodeOf(Runnable action) {
    Throwable thrown = catchThrowable(action::run);
    assertThat(thrown).isInstanceOf(CustomException.class);
    return ((CustomException) thrown).getErrorCode();
  }

  private static Member givenMember(String name, Position position) {
    Member member = Member.builder().username(name).name(name).role(Role.USER).position(position).build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private static HandoverPackage givenPackage(Member owner) {
    HandoverPackage created = HandoverPackage.builder().predecessor(owner).name("package").build();
    ReflectionTestUtils.setField(created, "id", UUID.randomUUID());
    return created;
  }

  private static AuthenticatedMember requesterOf(Member member) {
    return new AuthenticatedMember(member.getId(), member.getUsername(), member.getRole(), member.getPosition());
  }
}
