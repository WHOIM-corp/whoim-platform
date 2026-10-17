package com.chuseok22.whoim.server.admin.application.dto.response;

import com.chuseok22.whoim.server.admin.application.service.AdminPaging;
import java.util.List;
import java.util.UUID;

// packageOptions가 비어 있으면 필터 영역을 숨긴다
public record AdminListResponse(
    String tableName,
    List<String> columns,
    List<AdminRowResponse> rows,
    int page,
    int totalPages,
    long totalElements,
    UUID selectedPackageId,
    List<AdminPackageOptionResponse> packageOptions
) {

  public int pageSize() {
    return AdminPaging.PAGE_SIZE;
  }

  public boolean hasPrevious() {
    return page > 0;
  }

  public boolean hasNext() {
    return page + 1 < totalPages;
  }

  public long firstRowNumber() {
    return rows.isEmpty() ? 0 : (long) page * pageSize() + 1;
  }

  public long lastRowNumber() {
    return rows.isEmpty() ? 0 : firstRowNumber() + rows.size() - 1;
  }
}
