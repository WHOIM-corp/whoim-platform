package com.chuseok22.whoim.server.card.infrastructure.repository;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TraceCardRepository extends JpaRepository<TraceCard, UUID> {

  List<TraceCard> findAllByHandoverPackage(HandoverPackage handoverPackage);

  List<TraceCard> findAllByHandoverPackageIn(List<HandoverPackage> handoverPackages);

  List<TraceCard> findAllByHandoverPackagePredecessorOrderByUpdatedAtDesc(Member predecessor);

  List<TraceCard> findAllByStatusNotOrderByUpdatedAtDesc(CardStatus status);

  // 후임자 상담의 Hard Rule: 지정된 후임자의 PUBLISHED Package에 속한 APPROVED Card만 조회한다
  @Query("""
      select c from TraceCard c
      where c.status = :cardStatus
        and c.handoverPackage.status = :packageStatus
        and c.handoverPackage.successor.id = :successorId
      order by c.updatedAt desc
      """)
  List<TraceCard> findAllForSuccessor(
      @Param("cardStatus") CardStatus cardStatus,
      @Param("packageStatus") HandoverStatus packageStatus,
      @Param("successorId") UUID successorId);

  boolean existsByInterview(Interview interview);
}
