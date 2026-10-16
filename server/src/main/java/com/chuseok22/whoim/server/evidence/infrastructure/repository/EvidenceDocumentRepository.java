package com.chuseok22.whoim.server.evidence.infrastructure.repository;

import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceDocumentRepository extends JpaRepository<EvidenceDocument, UUID> {
}
