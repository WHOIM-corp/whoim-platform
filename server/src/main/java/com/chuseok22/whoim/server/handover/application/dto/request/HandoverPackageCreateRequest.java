package com.chuseok22.whoim.server.handover.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record HandoverPackageCreateRequest(
    @NotNull(message = "전임자를 선택해 주세요")
    UUID predecessorId,

    UUID successorId,

    @NotBlank(message = "Package 이름을 입력해 주세요")
    @Size(max = 100, message = "Package 이름은 100자 이하여야 합니다")
    String name,

    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다")
    String description
) {

}
