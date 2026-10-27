package com.chuseok22.whoim.server.member.application.service;

import com.chuseok22.whoim.server.member.application.dto.response.MemberSearchResponse;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

  public static final int DEFAULT_LIMIT = 6;
  public static final int MAX_LIMIT = 20;

  private final MemberRepository memberRepository;

  @Transactional(readOnly = true)
  public List<MemberSearchResponse> search(Position position, String keyword, int limit) {
    int clampedLimit = Math.clamp(limit, 1, MAX_LIMIT);
    return memberRepository.searchByPosition(position, toLikePattern(keyword), PageRequest.of(0, clampedLimit))
        .stream()
        .map(MemberSearchResponse::from)
        .toList();
  }

  // 키워드의 %, _, \ 가 와일드카드로 동작하지 않고 글자 그대로 검색되게 한다. '\'를 먼저 바꿔야 이중 이스케이프를 피한다
  private static String toLikePattern(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return "%";
    }
    String escaped = keyword.strip().toLowerCase(Locale.ROOT)
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
    return "%" + escaped + "%";
  }
}
