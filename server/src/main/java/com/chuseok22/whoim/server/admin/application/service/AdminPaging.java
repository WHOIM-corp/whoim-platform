package com.chuseok22.whoim.server.admin.application.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class AdminPaging {

  public static final int PAGE_SIZE = 50;
  // offset(page * PAGE_SIZE)이 int 범위를 넘어 500이 나지 않도록 page 상한을 둔다
  private static final int MAX_PAGE = Integer.MAX_VALUE / PAGE_SIZE;

  private AdminPaging() {
  }

  public static PageRequest pageRequest(int page) {
    int boundedPage = Math.min(Math.max(page, 0), MAX_PAGE);
    // createdAt이 같은 행의 순서를 id로 고정해 페이지 경계가 흔들리지 않게 한다
    return PageRequest.of(boundedPage, PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
  }
}
