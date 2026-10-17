package com.chuseok22.whoim.server.admin.application.dto.response;

import java.util.List;
import java.util.UUID;

public record AdminRowResponse(UUID id, List<AdminCell> cells) {
}
