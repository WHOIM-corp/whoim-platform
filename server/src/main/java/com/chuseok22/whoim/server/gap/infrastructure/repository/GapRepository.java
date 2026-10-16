package com.chuseok22.whoim.server.gap.infrastructure.repository;

import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GapRepository extends JpaRepository<Gap, UUID> {
}
