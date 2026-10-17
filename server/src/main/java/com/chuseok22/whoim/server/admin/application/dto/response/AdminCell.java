package com.chuseok22.whoim.server.admin.application.dto.response;

import com.chuseok22.whoim.server.admin.core.constant.AdminTable;
import com.chuseok22.whoim.server.common.application.time.SeoulTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record AdminCell(String text, String linkTableName, UUID linkId, String badge, boolean empty) {

  private static final int TRUNCATE_LENGTH = 80;
  private static final int SHORT_ID_LENGTH = 8;
  private static final String ELLIPSIS = "…";
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public static AdminCell text(String text) {
    if (text == null) {
      return blank();
    }
    return new AdminCell(text, null, null, null, false);
  }

  public static AdminCell truncated(String text) {
    if (text == null) {
      return blank();
    }
    if (text.length() <= TRUNCATE_LENGTH) {
      return text(text);
    }
    return text(text.substring(0, TRUNCATE_LENGTH) + ELLIPSIS);
  }

  public static AdminCell link(AdminTable table, UUID id) {
    if (id == null) {
      return blank();
    }
    return new AdminCell(shortId(id), table.pathName(), id, null, false);
  }

  public static AdminCell plainId(UUID id) {
    if (id == null) {
      return blank();
    }
    return text(shortId(id));
  }

  public static AdminCell badge(String badge) {
    if (badge == null) {
      return blank();
    }
    return new AdminCell(badge, null, null, badge, false);
  }

  public static AdminCell time(Instant instant) {
    if (instant == null) {
      return blank();
    }
    return text(SeoulTime.toOffsetDateTime(instant).format(TIME_FORMAT));
  }

  public static AdminCell blank() {
    return new AdminCell(null, null, null, null, true);
  }

  public boolean hasLink() {
    return linkTableName != null && linkId != null;
  }

  private static String shortId(UUID id) {
    return id.toString().substring(0, SHORT_ID_LENGTH);
  }
}
