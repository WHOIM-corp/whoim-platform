package com.chuseok22.whoim.server.card.infrastructure.repository;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import java.util.UUID;

public interface EvidenceCountRow {

  UUID getCardId();

  EvidenceLinkType getType();

  Long getTotal();
}
