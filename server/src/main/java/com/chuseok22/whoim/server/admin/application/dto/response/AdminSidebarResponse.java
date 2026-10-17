package com.chuseok22.whoim.server.admin.application.dto.response;

import java.util.List;

public record AdminSidebarResponse(List<AdminTableSummaryResponse> tables, int settingCount) {
}
