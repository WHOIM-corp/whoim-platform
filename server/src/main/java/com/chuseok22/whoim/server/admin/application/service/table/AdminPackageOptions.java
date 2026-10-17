package com.chuseok22.whoim.server.admin.application.service.table;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminPackageOptionResponse;
import com.chuseok22.whoim.server.admin.core.constant.AdminTable;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminPackageOptions {

  private static final int SHORT_ID_LENGTH = 8;

  private final HandoverPackageRepository handoverPackageRepository;

  // Package 필터가 없는 테이블은 빈 목록을 돌려주고, 화면은 필터 영역을 숨긴다
  public List<AdminPackageOptionResponse> optionsFor(AdminTable table) {
    if (!table.packageFilterable()) {
      return List.of();
    }
    return handoverPackageRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(handoverPackage -> new AdminPackageOptionResponse(
            handoverPackage.getId(),
            handoverPackage.getName() + " (" + handoverPackage.getId().toString().substring(0, SHORT_ID_LENGTH) + ")"))
        .toList();
  }
}
