package com.chuseok22.whoim.server.card.infrastructure.repository;

import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceCardRepository extends JpaRepository<TraceCard, UUID> {
}
