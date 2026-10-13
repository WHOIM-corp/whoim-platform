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

- Auth: JWT(Access Token만, `Authorization` 헤더), 리프레시 토큰 없음. 만료는 시연 중 끊기지 않게 넉넉히 둔다.
- Accounts: 회원가입 없음. 초기 관리자 1명을 설정 값으로 생성하고 나머지는 관리자 화면에서 추가·역할 지정한다. 역할은 전임자, 후임자, 회사 관리자 3종이고 조직은 단일이다(멀티테넌시 없음).
- Authorization: 역할별 API 접근 제한 + 소유 제한(전임자는 자기 Card, 후임자는 지정된 Package의 Published Card만, 관리자는 전체). 세부 권한 매트릭스와 SSO는 하지 않는다.
- CORS: 구현 시 `https://www.whoim.chuseok22.com`과 로컬 Vite dev 주소만 허용한다.
- 현황: `SecurityConfig`는 아직 기본값(`formLogin`+`httpBasic`, `/actuator/health`만 `permitAll`)이다. JWT 구현 시 바꾸되 `/actuator/health`는 계속 열어 둔다(CD 헬스체크). Actuator 노출은 `health`뿐이다.

## LLM

- Provider: `LLMProvider` 인터페이스 뒤에 SUH AI(Gemma 4 E4B, 전용 어댑터, 연동 방식은 추후 정리)를 기본으로, Gemini Flash-Lite급을 fallback으로 둔다. 모델명은 설정 값으로 두고 코드에 박지 않는다. API 키는 `application-*.yml`에만 둔다.
- Routing: 작업별 Provider를 설정으로 고른다(`llm.interview.provider`, `llm.card.provider`). 인터뷰·Card 생성 구현 직전에 시연 시나리오 하나를 같은 프롬프트로 Gemma와 fallback에 돌려 JSON 유효율, TRACE 필드 충족, 한국어 품질을 비교하고 라우팅을 확정한다.
- Flow: 인터뷰 턴마다 `{다음 질문, 추출한 TRACE 요소, 비어 있는 요소}` JSON을 받고, 서버 룰(TRACE 완성도 체크리스트)이 종료를 판단하며, 종료 시 Card를 1회 생성한다.
- Failure: 응답은 항상 스키마로 검증한다. 실패하면 같은 Provider로 1회 재시도하고, 그다음 fallback으로 넘긴다. 응답한 Provider는 로그에만 남기고 화면에는 노출하지 않는다.
- Budget: GPU 자원이 작다. 호출은 작고 직렬로 하며 병렬 호출은 하지 않는다. 인터뷰 턴은 누적 TRACE 상태 요약 + 직전 답변 + 남은 Gap 1~2개만, Card 생성은 TRACE 상태 요약 + Evidence 발췌만, 후임자 호출은 Card 요약 필드만 보낸다. 타임아웃과 출력 토큰 상한을 둔다. 목표는 인터뷰 1건 4~6회, 후임자 상담 1건 2회다.
- Parser: 응답 파서는 thought 블록이 있든 없든 견뎌야 한다(Gemma E4B는 thinking을 끄면 빈 thought 블록이 생기지 않는다).
- Data: 외부 클라우드 fallback에는 합성 데이터만 보낸다.

## Build

- `bootJar`는 `app.jar`로 고정하고 plain `jar`는 비활성이다. Dockerfile이 `build/libs/app.jar`를 복사하므로 이 설정을 바꾸지 않는다.
- `version`(`build.gradle`)은 릴리즈 봇이 관리하므로 직접 수정하지 않는다.
