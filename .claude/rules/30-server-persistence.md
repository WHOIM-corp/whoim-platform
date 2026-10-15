---
paths:
  - "server/**"
---

# Server Persistence

JPA 엔티티, 스키마(Flyway), Repository 규칙이다. 패키지 위치는 `10-server-architecture.md`, 시각 처리는 `20-server-coding-style.md`를 따른다. 참고 원본은 Big-Brother-BE이며 아래 차이만 우리 규칙이다.

## 규약

### 엔티티

- Base: 모든 엔티티는 `BaseEntity`(`common.infrastructure.persistence`)를 상속한다. `createdAt`/`updatedAt`은 `Instant`, JPA Auditing으로 채우고 시각은 `Clock`을 쓴다(차이점: BB는 `Clock` 없음).
- ID: 각 엔티티가 `@Id @GeneratedValue(strategy = GenerationType.UUID) UUID id`를 직접 선언한다. 필드명은 `id`라서 FK 컬럼이 `member_id` 형태가 된다.
- Naming: `@Table`/`@Column` 이름을 지정하지 않고 Boot 기본 네이밍에 맡긴다. 테이블은 소문자 snake_case 단수(`member`, `trace_card`).
- Lombok: `@Getter`, `@Setter` 허용, `@Data` 금지, `@NoArgsConstructor(access = PROTECTED)`.
- 생성: 상태와 ID를 받지 않는 private 생성자에 `@Builder`를 붙이고, 초기 상태는 생성자 안에서 정한다.
- 변경: 상태 변경은 이름 있는 도메인 메서드로 한다(Setter로 상태를 바꾸지 않는다).
- Delete: hard delete만 쓴다(soft delete 컬럼 없음).

### 연관관계

- 허용: `@ManyToOne(fetch = LAZY)`만 쓴다.
- 금지: `@OneToMany`와 JPA cascade. 자식 목록은 자식 Repository에서 부모 기준으로 조회하고, 저장·삭제는 서비스가 명시적으로 호출한다.
- 예외: 마이그레이션 SQL의 DB 수준 `ON DELETE CASCADE`는 허용한다.

### enum과 상태 전이

- Mapping: `@Enumerated(EnumType.STRING)`, DB는 `TEXT` + `CHECK`. `ORDINAL` 금지.
- Rule: 전이 규칙은 상태 enum(`core.constant`)의 메서드(`canTransitionTo` 등)에 둔다. 서비스가 검증한 뒤 엔티티 메서드로 적용한다.
- Violation: 허용되지 않은 전이는 `CustomException(ErrorCode.STATE_CONFLICT)`로 거부한다.
- History: 전이가 성공하면 이력 테이블(`*_state_history`)에 한 줄을 남긴다.

### Flyway

- Source: Flyway SQL이 스키마의 유일한 원본이다. `ddl-auto`는 `validate`이며 사용자가 yml에서 설정한다.
- Location: `server/src/main/resources/db/migration`.
- Name: `V<yyyyMMdd>_<HHmmss>__<설명>.sql`, 시각은 작성 시점 KST(예: `V20261015_170502__init.sql`).
- Immutable: 적용된 파일은 수정하지 않는다. 변경은 새 파일로 추가하고 이슈별로 증분 마이그레이션을 쓴다.
- Types: `UUID`, `TEXT`, `TIMESTAMPTZ`, `CHECK`를 쓴다.
- Author: 마이그레이션 파일은 에이전트가 작성한다. DB 초기화·drop·truncate·볼륨 삭제는 하지 않는다(필요하면 원인과 절차만 안내).
- Dependencies: `spring-boot-starter-flyway` + `flyway-database-postgresql`(버전은 Boot BOM이 관리, 이슈 #2에서 도입).

### member 테이블 제약

- Columns: `role`(`USER`/`ADMIN`), `position`(`PREDECESSOR`/`SUCCESSOR`/`REVIEWER`, nullable). 의미는 `50-server-auth-security.md`.
- Check: `CHECK ((role = 'ADMIN' AND position IS NULL) OR (role = 'USER' AND position IS NOT NULL))`. `position` 값 목록도 별도 `CHECK`로 제한한다.

### Repository

- Location: 도메인의 `infrastructure` 패키지. `MemberRepository`처럼 단일 인터페이스로 시작하고 `Custom`/`Impl` 구조는 필요할 때만 만든다.
- Query: 기본은 Spring Data 파생 쿼리. 어려운 경우에만 JPQL·native를 쓰고, 복잡한 조회 결과는 record나 인터페이스 프로젝션으로 받는다.

### 소유 테이블

- 요약: member / handover_package, handover_state_history / trace_card, card_revision, card_evidence_link, card_state_history / interview, interview_turn / evidence_document / gap. 도메인별 소유는 `10-server-architecture.md`.

## 현재 코드 상태

- 이슈 #2에서 추가: `Member`(`member.infrastructure.entity`, 필드 `id`, `username`(unique), `password`, `name`, `role`, `position`), `MemberRepository`(`findByUsername`, `existsByUsername`), `BaseEntity`(`createdAt`/`updatedAt` `Instant`), `JpaAuditingConfig`(`Clock` 기반 `DateTimeProvider`), 마이그레이션 `V20261015_230147__create_member.sql`.
- `Member`는 role-position 조합을 검증하지 않는다. 조합은 `MemberSeedValidator`와 DB `CHECK`가 막는다.
- 마이그레이션은 컬럼 `UUID`/`TEXT`/`TIMESTAMPTZ`, `role`·`position`·조합 `CHECK` 3개, `username` UNIQUE를 정의한다. 다른 엔티티·Repository·마이그레이션은 아직 없다.
- 검증 필요(미검증): 실제 DB 실행 전이라 Hibernate 7 `validate`와 `TEXT`+`CHECK`/`TIMESTAMPTZ`/`UUID` 조합, Flyway 마이그레이션 적용은 확인하지 못했다. Flyway 좌표는 `./gradlew compileJava`가 통과하는 것(의존성 해석)까지만 확인했다.
