package com.chuseok22.whoim.server.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class AdminPagingTest {

  @Test
  void negativePage_becomesZero() {
    assertThat(AdminPaging.pageRequest(-1).getPageNumber()).isZero();
  }

  @Test
  void pageSize_isFifty() {
    assertThat(AdminPaging.pageRequest(2).getPageSize()).isEqualTo(50);
  }

  @Test
  void sort_isCreatedAtDescendingThenIdDescending() {
    Sort sort = AdminPaging.pageRequest(0).getSort();

    Sort.Order createdAt = sort.getOrderFor("createdAt");
    Sort.Order id = sort.getOrderFor("id");
    assertThat(createdAt).isNotNull();
    assertThat(createdAt.getDirection()).isEqualTo(Sort.Direction.DESC);
    assertThat(id).isNotNull();
    assertThat(id.getDirection()).isEqualTo(Sort.Direction.DESC);
    assertThat(sort.stream().map(Sort.Order::getProperty)).containsExactly("createdAt", "id");
  }

  @Test
  void maxIntPage_isClampedSoOffsetFitsInInt() {
    assertThatCode(() -> AdminPaging.pageRequest(Integer.MAX_VALUE)).doesNotThrowAnyException();

    assertThat(AdminPaging.pageRequest(Integer.MAX_VALUE).getOffset()).isLessThanOrEqualTo(Integer.MAX_VALUE);
  }
}
