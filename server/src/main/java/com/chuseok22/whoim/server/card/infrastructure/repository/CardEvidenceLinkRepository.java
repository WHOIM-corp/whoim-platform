package com.chuseok22.whoim.server.card.infrastructure.repository;

import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardEvidenceLinkRepository extends JpaRepository<CardEvidenceLink, UUID> {

  @EntityGraph(attributePaths = {"turn", "document"})
  List<CardEvidenceLink> findAllByCardOrderByCreatedAtAsc(TraceCard card);

  boolean existsByCard(TraceCard card);
}
