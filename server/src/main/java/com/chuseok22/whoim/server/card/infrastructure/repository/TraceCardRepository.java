package com.chuseok22.whoim.server.card.infrastructure.repository;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceCardRepository extends JpaRepository<TraceCard, UUID> {

  List<TraceCard> findAllByHandoverPackage(HandoverPackage handoverPackage);

  List<TraceCard> findAllByHandoverPackageIn(List<HandoverPackage> handoverPackages);

  List<TraceCard> findAllByHandoverPackagePredecessorOrderByUpdatedAtDesc(Member predecessor);

  List<TraceCard> findAllByStatusNotOrderByUpdatedAtDesc(CardStatus status);

  boolean existsByInterview(Interview interview);
}
