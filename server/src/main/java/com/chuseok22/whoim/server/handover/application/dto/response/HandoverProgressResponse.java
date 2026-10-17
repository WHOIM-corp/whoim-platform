package com.chuseok22.whoim.server.handover.application.dto.response;

import com.chuseok22.whoim.server.handover.application.dto.data.HandoverProgress;

public record HandoverProgressResponse(
    int approvedCount,
    int activeCount,
    double ratio
) {

  public static HandoverProgressResponse from(HandoverProgress progress) {
    double ratio = progress.activeCount() == 0 ? 0.0 : (double) progress.approvedCount() / progress.activeCount();
    return new HandoverProgressResponse(progress.approvedCount(), progress.activeCount(), ratio);
  }
}
