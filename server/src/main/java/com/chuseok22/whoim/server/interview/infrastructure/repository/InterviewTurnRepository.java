package com.chuseok22.whoim.server.interview.infrastructure.repository;

import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewTurnRepository extends JpaRepository<InterviewTurn, UUID> {

  List<InterviewTurn> findAllByInterviewOrderByTurnNumberAsc(Interview interview);

  Optional<InterviewTurn> findTopByInterviewOrderByTurnNumberDesc(Interview interview);

  long countByInterviewAndAnswerIsNotNull(Interview interview);
}
