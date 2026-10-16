package com.chuseok22.whoim.server.handover.infrastructure.repository;

import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoverPackageRepository extends JpaRepository<HandoverPackage, UUID> {
}
