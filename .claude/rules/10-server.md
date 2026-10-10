---
paths:
  - "server/**"
---

# Server

## High-level architecture

- Module: 단일 Gradle(Groovy) 모듈(`server/`)이다. Spring Boot 4.1.1 / Java 21 toolchain.
- Package root: `com.chuseok22.whoim.server` (현재 `ServerApplication`, `config/SecurityConfig`만 존재)
- Structure: 도메인형 `domain/<name>/{controller,service,repository,entity,dto}`. 공통 설정은 `config/`에 둔다.
- Dependencies: actuator, data-jpa, security, thymeleaf(+ `thymeleaf-extras-springsecurity6`), webmvc, PostgreSQL 드라이버. Thymeleaf는 의존성에 포함(용도 미정).
- New dependency: `lombok`, `starter-validation`, springdoc 등은 필요해지는 시점에 사용자 확인 후 추가한다. springdoc은 프론트 연동을 시작할 때 도입하므로 지금은 추가하지 않는다.

## Conventions

- Style: 들여쓰기 2칸, 포매터(Spotless 등) 없음. 주석은 한국어로 "왜"가 필요한 곳에만 쓴다.
- DTO: Java `record`. Lombok은 엔티티에만 쓴다(도입은 사용자 확인 후).
- Configuration: 설정값은 `@Value` 대신 `@ConfigurationProperties`로 바인딩한다.
- API: prefix `/api`, 버전 경로 없음, JSON 필드 camelCase.
- Policy override: 성공 응답은 envelope 없이 DTO를 그대로 반환하고, 에러는 도메인별 `ErrorCode` + Spring `ProblemDetail`로 응답한다(전역 common/patterns의 API Response Format을 따르지 않는다).

## Time

- Storage: 서버·DB는 `Instant`(UTC)로 다룬다. 컨테이너 `TZ=Asia/Seoul`.
- Response: API 응답은 KST 오프셋(`+09:00`) ISO-8601이다. 응답 DTO의 시각 필드는 `OffsetDateTime`(또는 `ZonedDateTime`)으로 `Asia/Seoul` 변환해 반환하고, `Instant`를 응답에 그대로 노출하지 않는다.
- Caution: `spring.jackson.time-zone`은 `Instant` 직렬화(`...Z`)에 영향을 주지 않으므로 이 설정으로 `+09:00`을 만들 수 없다.

## Database

- 목표: `application-dev.yml`을 채울 때 `spring.jpa.hibernate.ddl-auto: update`를 쓰고, 로컬은 로컬 컨테이너 PostgreSQL, 배포는 서버의 PostgreSQL 컨테이너를 쓴다. Flyway는 쓰지 않는다.
- Present: `application-dev.yml`과 `application-prod.yml`은 현재 비어 있고(0 bytes), `application.yml`에는 datasource·profile이 없다. 사용자가 dev yml을 채우고 dev 프로필로 실행하기 전에는 `bootRun`과 `ServerApplicationTests#contextLoads`가 동작하지 않는다.
- Caution: `ddl-auto: update`는 컬럼/테이블 삭제·이름 변경을 반영하지 않는다. 엔티티 필드 삭제·이름 변경은 사용자 승인 후에만 한다.
- Reset: 스키마 불일치 시 DB 초기화는 항상 사용자가 직접 한다. Claude는 원인과 절차만 안내하고 drop/truncate/볼륨 삭제를 하지 않는다.

## Configuration files

- `application.yml`: 추적됨, 공통 설정만 둔다(민감정보 금지).
- `application-dev.yml`: 로컬 실행용, `application-prod.yml`: 배포용. 둘 다 gitignore이며 수정은 사용자 승인 후에만 한다. 값은 출력하지 않는다(상세는 `50-secrets-and-env.md`).
- Prod: CI/CD가 `APPLICATION_PROD_YML` 시크릿으로 prod 파일을 생성한다.

## Security

- 현황: `SecurityConfig`는 `/actuator/health`만 `permitAll`, 나머지는 인증 필요, `formLogin`+`httpBasic` 기본값이다. Actuator 노출은 `health`뿐이다.
- 인증·권한 구조는 추후 논의 — 확정 전 사용자 지시 없이 구현하거나 `SecurityConfig`를 바꾸지 않는다.

## Build

- `bootJar`는 `app.jar`로 고정하고 plain `jar`는 비활성이다. Dockerfile이 `build/libs/app.jar`를 복사하므로 이 설정을 바꾸지 않는다.
- `version`(`build.gradle`)은 릴리즈 봇이 관리하므로 직접 수정하지 않는다.
