package com.chuseok22.whoim.server.admin.application.dto.response;

import java.util.List;

// kind는 TABLE 또는 CONVERSATION
public record AdminSectionResponse(String title, String kind, List<String> columns, List<AdminRowResponse> rows) {
}
