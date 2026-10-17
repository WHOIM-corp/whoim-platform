package com.chuseok22.whoim.server.handover.application.dto.request;

import java.util.UUID;

// successorId가 null이면 후임자 지정을 해제한다
public record HandoverPackageSuccessorRequest(UUID successorId) {

}
