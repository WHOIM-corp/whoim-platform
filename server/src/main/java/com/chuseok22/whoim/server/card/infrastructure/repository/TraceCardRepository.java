package com.chuseok22.whoim.server.card.infrastructure.repository;

import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceCardRepository extends JpaRepository<TraceCard, UUID> {

  List<TraceCard> findAllByHandoverPackage(HandoverPackage handoverPackage);

  boolean existsByInterview(Interview interview);
}
