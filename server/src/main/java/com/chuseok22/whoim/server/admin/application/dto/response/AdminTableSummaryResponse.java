package com.chuseok22.whoim.server.admin.application.dto.response;

public record AdminTableSummaryResponse(
    String tableName,
    String description,
    String childrenNote,
    long rowCount,
    AdminCell lastCreatedAt
) {
}
