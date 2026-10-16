package com.chuseok22.whoim.server.card.infrastructure.repository;

import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardEvidenceLinkRepository extends JpaRepository<CardEvidenceLink, UUID> {
}
