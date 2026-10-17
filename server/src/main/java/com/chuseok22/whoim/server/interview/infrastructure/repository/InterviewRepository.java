package com.chuseok22.whoim.server.interview.infrastructure.repository;

import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {

  List<Interview> findAllByHandoverPackage(HandoverPackage handoverPackage);

  boolean existsByGap(Gap gap);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Interview> findWithLockById(UUID id);

  Page<Interview> findAllByHandoverPackageId(UUID handoverPackageId, Pageable pageable);
}
