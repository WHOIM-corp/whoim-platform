# Architecture And Boundaries

## High-level architecture

- 모노레포. server는 단일 Gradle 모듈의 도메인 주도 패키지 구조이고, web은 feature 단위다.
- 계층: Controller → Service → Repository. Controller는 Service만 호출하고 Repository를 직접 호출하지 않는다.
- 외부 연동: PostgreSQL, LLM(Gemini, SUH AI). 교체 가능한 외부 연동(`LLMProvider`)에만 인터페이스를 둔다. 구현이 하나뿐인 서비스에는 인터페이스를 만들지 않는다.

## Module boundaries

| 도메인 | 책임 |
| --- | --- |
| common | 공통 예외, 시각, 설정, BaseEntity |
| member | 계정, 후임자 선택용 조회 |
| auth | 로그인, JWT, 보안 설정 |
| handover | Handover Package, 진행률, Publish |
| card | TRACE Card, 근거 연결, 상태 이력, 검수 |
| interview | Phronesis Interview |
| evidence | 문서 업로드 |
| gap | Gap 탐지, 근거 앵커 |
| consultation | 후임자 Guided Decision Support(테이블 없음) |
| llm | LLM Provider 호출 |
| dlp | 민감정보 탐지·마스킹·복원 |
| setting | 서비스 설정 값 key-value, 설정 키 레지스트리, ADMIN API |

- 의존: 다른 도메인의 클래스를 import할 수 있고 엔티티 간 FK 참조의 순환은 허용한다. `common`은 다른 도메인에 의존하지 않는다. `llm`은 독립 도메인이라 다른 도메인이 `llm`을 쓰고 `llm`은 이들을 모른다. 단 `llm`은 `LlmInputGuard` 인터페이스로만 `dlp`와 만난다.
- 금지: 서버·웹 소스의 패키지·디렉터리 이름 `docs`, `issue`, `report`(`.gitignore`가 무시해 소스가 추적에서 빠진다). 루트 `docs/`는 예외다(로컬 작업 공간).

## Data flow

- 요청·응답: Controller → Service(`@Transactional`) → Repository. 엔티티를 API 응답으로 노출하지 않고 DTO로만 응답한다.
- LLM 호출 원칙: 직렬 단일 호출로 작게(TRACE 요약과 근거 발췌만), 같은 Provider 재시도 없이 다음 Provider로 한 번만 전환하고, 생성 입력은 DLP 마스킹 후 전송하며 응답은 복원한다. 실패하면 `LLM_UNAVAILABLE`(503)/`DLP_UNAVAILABLE`, 응답은 항상 스키마로 검증한다. 트랜잭션 밖에서 호출하고 전후로 트랜잭션을 나눈다. 인터뷰 종료 판단은 LLM이 아니라 서버 룰이 한다.
- LLM 설정: 모델명·엔드포인트는 설정 값이다. API 키는 `application-*.yml`에만 둔다. 외부(Gemini)로 나가는 모든 입력은 DLP(Rule + AI)로 마스킹된 텍스트만 보낸다(시연·테스트 데이터는 합성 데이터). 로그에는 provider, 모델, 지표, 성공 여부만 남기고 prompt·응답 본문·키는 남기지 않는다.
- 시각: 저장은 `Instant`, 현재 시각은 `Clock` 빈, 응답은 `SeoulTime`으로 변환한 `OffsetDateTime`(`+09:00`).
- 비동기 처리, 캐시, 큐는 없다.

## File / folder conventions

- server 도메인 패키지:

  ```text
  com.chuseok22.whoim.server.{domain}
  ├── core.constant            enum, 상수, 상태 전이 규칙
  ├── application
  │   ├── controller           XController, XControllerDocs(같은 패키지)
  │   ├── dto.{request,response,data}
  │   └── service
  └── infrastructure
      ├── entity, repository
      ├── adapter              외부 연동 구현체(llm)
      ├── properties, config
  ```

- 도메인 패키지는 해당 도메인을 구현할 때 만든다. 빈 패키지를 미리 만들지 않는다.
- 엔티티와 Repository는 `infrastructure`, Controller는 `application`에 둔다.
- web: `src/features/<name>/{components,hooks,api,...}`, 공용 코드는 `src/shared`.

## Extension points

- 새 기능은 기존 `member`·`auth` 도메인의 구조와 `Member` 엔티티를 먼저 참고한다.
- 공통 코드(`ErrorCode`, `CustomException`, `GlobalExceptionHandler`, `BaseEntity`, `SeoulTime`)를 재사용하고 같은 역할의 새 클래스를 만들지 않는다.
