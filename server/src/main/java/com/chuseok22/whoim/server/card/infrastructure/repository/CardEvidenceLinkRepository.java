package com.chuseok22.whoim.server.card.infrastructure.repository;

import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardEvidenceLinkRepository extends JpaRepository<CardEvidenceLink, UUID> {

  @EntityGraph(attributePaths = {"turn", "document"})
  List<CardEvidenceLink> findAllByCardOrderByCreatedAtAsc(TraceCard card);

  boolean existsByCard(TraceCard card);

  @Query("select l.card.id as cardId, l.type as type, count(l) as total "
      + "from CardEvidenceLink l where l.card.id in :cardIds group by l.card.id, l.type")
  List<EvidenceCountRow> countByCardIds(@Param("cardIds") Collection<UUID> cardIds);
}
