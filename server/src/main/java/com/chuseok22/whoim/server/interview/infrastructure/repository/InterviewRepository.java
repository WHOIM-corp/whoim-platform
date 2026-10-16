package com.chuseok22.whoim.server.interview.infrastructure.repository;

import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {
}
