package com.chuseok22.whoim.server.member.infrastructure.repository;

import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

  Optional<Member> findByUsername(String username);

  boolean existsByUsername(String username);
}
