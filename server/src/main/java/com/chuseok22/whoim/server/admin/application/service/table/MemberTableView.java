package com.chuseok22.whoim.server.admin.application.service.table;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminCell;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminFieldResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminRowResponse;
import com.chuseok22.whoim.server.admin.application.service.AdminPaging;
import com.chuseok22.whoim.server.admin.core.constant.AdminTable;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// password는 View 모델에 넣지 않는다(마스킹이 아니라 필드 자체를 두지 않는다)
@Component
@RequiredArgsConstructor
public class MemberTableView implements AdminTableView {

  private static final List<String> COLUMNS = List.of("id", "username", "name", "role", "position", "created_at");

  private final MemberRepository memberRepository;

  @Override
  public AdminTable table() {
    return AdminTable.MEMBER;
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return memberRepository.count();
  }

  @Override
  @Transactional(readOnly = true)
  public Instant lastCreatedAt() {
    return memberRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
        .stream().findFirst().map(Member::getCreatedAt).orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public AdminListResponse list(int page, UUID handoverPackageId) {
    Page<Member> members = memberRepository.findAll(AdminPaging.pageRequest(page));
    List<AdminRowResponse> rows = members.getContent().stream()
        .map(member -> new AdminRowResponse(member.getId(), List.of(
            AdminCell.link(AdminTable.MEMBER, member.getId()),
            AdminCell.text(member.getUsername()),
            AdminCell.text(member.getName()),
            AdminCell.badge(member.getRole().name()),
            AdminCell.badge(member.getPosition() == null ? null : member.getPosition().name()),
            AdminCell.time(member.getCreatedAt()))))
        .toList();
    return new AdminListResponse(AdminTable.MEMBER.pathName(), COLUMNS, rows, members.getNumber(),
        members.getTotalPages(), members.getTotalElements(), null, List.of());
  }

  @Override
  @Transactional(readOnly = true)
  public AdminDetailResponse detail(UUID id) {
    Member member = memberRepository.findById(id)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    return new AdminDetailResponse(AdminTable.MEMBER.pathName(), member.getId(), List.of(
        new AdminFieldResponse("id", AdminCell.text(member.getId().toString())),
        new AdminFieldResponse("username", AdminCell.text(member.getUsername())),
        new AdminFieldResponse("name", AdminCell.text(member.getName())),
        new AdminFieldResponse("role", AdminCell.badge(member.getRole().name())),
        new AdminFieldResponse("position", AdminCell.badge(member.getPosition() == null ? null : member.getPosition().name())),
        new AdminFieldResponse("created_at", AdminCell.time(member.getCreatedAt())),
        new AdminFieldResponse("updated_at", AdminCell.time(member.getUpdatedAt()))), List.of());
  }
}
