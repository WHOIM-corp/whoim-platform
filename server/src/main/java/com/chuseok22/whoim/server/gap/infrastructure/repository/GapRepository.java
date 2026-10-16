package com.chuseok22.whoim.server.gap.infrastructure.repository;

import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GapRepository extends JpaRepository<Gap, UUID> {

  boolean existsByHandoverPackage(HandoverPackage handoverPackage);
}
