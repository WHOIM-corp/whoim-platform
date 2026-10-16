package com.chuseok22.whoim.server.interview.infrastructure.repository;

import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewTurnRepository extends JpaRepository<InterviewTurn, UUID> {
}
