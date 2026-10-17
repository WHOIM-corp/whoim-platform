package com.chuseok22.whoim.server.admin.application.service;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminCell;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSidebarResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminTableSummaryResponse;
import com.chuseok22.whoim.server.admin.application.service.table.AdminTableView;
import com.chuseok22.whoim.server.admin.core.constant.AdminTable;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.setting.application.dto.response.SettingConfigResponse;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

// View 변환은 각 AdminTableView의 읽기 전용 트랜잭션 안에서 끝나므로 이 서비스에는 @Transactional이 없다
@Service
public class AdminTableService {

  private final List<AdminTableView> tableViews;
  private final SettingConfigService settingConfigService;

  public AdminTableService(List<AdminTableView> tableViews, SettingConfigService settingConfigService) {
    this.tableViews = tableViews.stream()
        .sorted(Comparator.comparing(AdminTableView::table))
        .toList();
    this.settingConfigService = settingConfigService;
  }

  // 등록된 AdminTableView만 요약한다. 뷰가 추가되면 자동으로 따라온다
  public List<AdminTableSummaryResponse> summaries() {
    return tableViews.stream()
        .map(view -> new AdminTableSummaryResponse(
            view.table().pathName(),
            view.table().description(),
            view.table().childrenNote(),
            view.count(),
            AdminCell.time(view.lastCreatedAt())))
        .toList();
  }

  public AdminSidebarResponse sidebar() {
    return new AdminSidebarResponse(summaries(), SettingKey.values().length);
  }

  public long overriddenSettingCount() {
    return settingConfigService.list().stream()
        .filter(SettingConfigResponse::overridden)
        .count();
  }

  public AdminListResponse list(String tableName, int page, UUID handoverPackageId) {
    AdminTableView view = findView(tableName);
    UUID effectivePackageId = view.table().packageFilterable() ? handoverPackageId : null;
    return view.list(page, effectivePackageId);
  }

  public AdminDetailResponse detail(String tableName, UUID id) {
    return findView(tableName).detail(id);
  }

  private AdminTableView findView(String tableName) {
    AdminTable table = AdminTable.fromPathName(tableName)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    return tableViews.stream()
        .filter(view -> view.table() == table)
        .findFirst()
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
