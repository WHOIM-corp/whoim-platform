package com.chuseok22.whoim.server.handover.infrastructure.repository;

import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface HandoverPackageRepository extends JpaRepository<HandoverPackage, UUID> {

  boolean existsByPredecessorAndName(Member predecessor, String name);

  List<HandoverPackage> findAllByOrderByCreatedAtDesc();

  List<HandoverPackage> findAllByPredecessorOrderByCreatedAtDesc(Member predecessor);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<HandoverPackage> findWithLockById(UUID id);
}
