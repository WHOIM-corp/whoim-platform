package com.chuseok22.whoim.server.gap.infrastructure.repository;

import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GapAnchorRepository extends JpaRepository<GapAnchor, UUID> {
}
