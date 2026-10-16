package com.chuseok22.whoim.server.evidence.infrastructure.repository;

import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceDocumentRepository extends JpaRepository<EvidenceDocument, UUID> {

  List<EvidenceDocument> findAllByHandoverPackageOrderByCreatedAtAsc(HandoverPackage handoverPackage);
}
