---
paths:
  - "server/**"
---

# Server Architecture

## Structure

- Module: 단일 Gradle(Groovy) 모듈(`server/`)의 도메인 주도(DDD) 구조다. Spring Boot 4.1.1, Java 21.
- Package root: `com.chuseok22.whoim.server`. 도메인별로 패키지를 나눈다.
- Reference: 구조는 Big-Brother-BE 규칙을 참고하되 이 프로젝트에 맞게 정했다.

## Domains

| 도메인 | 책임 | 소유 테이블 |
| --- | --- | --- |
| common | 공통 예외, 시각, 설정, BaseEntity | 없음 |
| member | 계정, 후임자 선택용 조회 | member |
| auth | 로그인, JWT, 보안 설정 | 없음 |
| handover | Handover Package, 진행률, Publish | handover_package, handover_state_history |
| card | TRACE Card, 수정 이력, 검수 | trace_card, card_revision, card_evidence_link, card_state_history |
| interview | Phronesis Interview | interview, interview_turn |
| evidence | Evidence 업로드 | evidence_document |
| gap | Gap Detection | gap |
| consultation | 후임자 Guided Decision Support (테이블 없음) | 없음 |
| llm | LLM Provider 호출과 오케스트레이션 | 없음 |

- Naming: 테이블 이름은 소문자 snake_case 단수형이다. 각 테이블은 소유 도메인이 정의한다.
- Excluded: setting, admin 도메인은 없다. 서비스 설정 동적 관리와 ADMIN 기능은 나중이다.

## Domain package

```text
com.chuseok22.whoim.server.{domain}
├── core
│   └── constant          enum, 상수, 상태 전이 규칙
├── application
│   ├── controller        XController, XControllerDocs
│   ├── dto
│   │   ├── request       요청 DTO
│   │   ├── response      응답 DTO
│   │   └── data          내부 전달용 DTO
│   └── service           서비스
└── infrastructure
    ├── entity            JPA 엔티티
    ├── repository        Spring Data Repository
    ├── properties        @ConfigurationProperties
    └── config            도메인 전용 설정, 외부 연동 어댑터
```

- Location: 엔티티와 Repository는 `infrastructure`에 둔다. Controller는 `application`에 두고 Service만 호출한다.
- Create: 도메인 패키지는 해당 도메인을 구현할 때 만든다. 빈 패키지를 미리 만들지 않는다.

## common

- Exception: `common.application.exception`에 `ErrorCode`, `CustomException`, `ErrorResponse`, `GlobalExceptionHandler`를 둔다.
- Exception to DTO rule: `ErrorResponse`가 `application.exception`에 있는 것은 DTO 패키지 규칙의 의도된 예외다.
- Time: `common.application.time.SeoulTime`(응답 변환 유틸), `common.infrastructure.config.ClockConfig`(`Clock` 빈).
- Persistence: `common.infrastructure.persistence.BaseEntity`, `common.infrastructure.config`에 `JpaAuditingConfig`, `OpenApiConfig` 등.
- Spring ErrorResponse: `org.springframework.web.ErrorResponse`와 이름이 같다. import를 혼동하지 않는다.
- Details: 에러는 `40-server-api-and-error.md`, 엔티티는 `30-server-persistence.md`를 따른다.

## Dependency rules

- Cross-domain: 다른 도메인의 클래스를 import할 수 있다. 순환 의존은 만들지 않는다.
- common: 다른 도메인에 의존하지 않는다.
- llm: 독립 도메인이다. interview, card, consultation이 llm을 쓰고 llm은 이들을 모른다.
- Call direction: Controller → Service → Repository. Controller가 Repository를 직접 호출하지 않는다.
- Interface: 교체 가능한 외부 연동(`LLMProvider`)에만 인터페이스를 둔다. 구현이 하나뿐인 서비스에 인터페이스를 만들지 않는다.

## Forbidden package names

- Names: `docs`, `issue`, `report`(`.docs`, `.issue`, `.report`도 같다)는 패키지·디렉터리 이름으로 쓰지 않는다.
- Why: 루트 `.gitignore`가 어느 깊이의 같은 이름 디렉터리든 무시해서 그 아래 소스가 git 추적에서 빠진다.
- Docs interface: `XControllerDocs`는 별도 패키지 없이 해당 Controller와 같은 패키지에 둔다.

## Current code state

- Present: 서버 코드는 `ServerApplication`, `config/SecurityConfig`, `auth.infrastructure.config.SecurityPaths`, 그리고 `common`(`application.exception`의 `ErrorCode`·`CustomException`·`ErrorResponse`·`GlobalExceptionHandler`, `application.time.SeoulTime`, `infrastructure.config`의 `ClockConfig`·`OpenApiConfig`)이다. 엔티티·`BaseEntity`·도메인 패키지는 아직 없다.
- Target: 위 구조는 규약이다. 새 코드는 이 구조로 만들고, 기존 `config/SecurityConfig`의 이동은 인증 이슈에서 한다.
