package com.chuseok22.whoim.server.admin.application.service.table;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.admin.core.constant.AdminTable;
import java.time.Instant;
import java.util.UUID;

public interface AdminTableView {

  AdminTable table();

  long count();

  // 행이 없으면 null
  Instant lastCreatedAt();

  AdminListResponse list(int page, UUID handoverPackageId);

  // 행이 없으면 CustomException(RESOURCE_NOT_FOUND)
  AdminDetailResponse detail(UUID id);
}
