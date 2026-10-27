package com.chuseok22.whoim.server.member.infrastructure.repository;

import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.core.constant.Position;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, UUID> {

  Optional<Member> findByUsername(String username);

  boolean existsByUsername(String username);

  // pattern은 호출자가 소문자로 만들고 LIKE 와일드카드를 '\'로 이스케이프한 값이다
  @Query("""
      select m from Member m
      where m.position = :position
        and (lower(m.name) like :pattern escape '\\'
          or lower(m.department) like :pattern escape '\\'
          or lower(m.jobTitle) like :pattern escape '\\'
          or lower(m.employeeNumber) like :pattern escape '\\')
      order by m.name asc, m.employeeNumber asc
      """)
  List<Member> searchByPosition(
      @Param("position") Position position, @Param("pattern") String pattern, Pageable pageable);
}
