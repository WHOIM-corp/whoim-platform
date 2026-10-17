package com.chuseok22.whoim.server.member.application.service;

import com.chuseok22.whoim.server.member.application.dto.response.MemberResponse;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

  private final MemberRepository memberRepository;

  @Transactional(readOnly = true)
  public List<MemberResponse> listByPosition(Position position) {
    return memberRepository.findAllByPositionOrderByNameAsc(position).stream()
        .map(MemberResponse::from)
        .toList();
  }
}
