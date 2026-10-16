package com.chuseok22.whoim.server.card.infrastructure.repository;

import com.chuseok22.whoim.server.card.infrastructure.entity.CardStateHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardStateHistoryRepository extends JpaRepository<CardStateHistory, UUID> {
}
