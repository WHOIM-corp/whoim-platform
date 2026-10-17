package com.chuseok22.whoim.server.card.infrastructure.repository;

import com.chuseok22.whoim.server.card.infrastructure.entity.CardStateHistory;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardStateHistoryRepository extends JpaRepository<CardStateHistory, UUID> {

  @EntityGraph(attributePaths = "actor")
  List<CardStateHistory> findAllByCardOrderByCreatedAtAsc(TraceCard card);
}
