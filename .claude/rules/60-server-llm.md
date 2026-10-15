---
paths:
  - "server/**"
---

# Server LLM

`llm` 도메인과 인터뷰·Card 생성에서 LLM을 부르는 규칙이다. 도메인 구조는 `10-server-architecture.md`, 설정 키 취급은 `50-server-auth-security.md`를 따른다.

## 규약

### 구조

- Domain: `llm` 도메인이 독립으로 둔다. 다른 도메인(interview, card 등)이 `llm`을 호출하고 `llm`은 다른 도메인에 의존하지 않는다.
- Interface: `LLMProvider`는 교체 가능한 외부 연동이라 인터페이스를 두는 유일한 예외다(`10-server-architecture.md`).
- Layout: `core.constant`(Provider 종류 등), `application`(오케스트레이션: 라우팅·검증·재시도·fallback), `infrastructure.adapter`(SUH AI 전용 어댑터, Gemini 어댑터, fake 구현).
- Fake: 테스트·시연 대비용 fake 구현을 둔다. 실제 호출 없이 흐름을 검증할 때 쓴다.

### Provider와 라우팅

- Default: SUH AI(Gemma 4 E4B, 전용 어댑터)를 기본으로, Gemini Flash-Lite급을 fallback으로 둔다.
- Config: 모델명·엔드포인트는 설정 값으로 두고 코드에 박지 않는다. API 키는 `application-*.yml`에만 두고 값은 문서에 쓰지 않는다. 키 이름은 추가 시 사용자에게 알린다.
- Routing: 작업별 Provider를 설정으로 고른다. 키: `llm.interview.provider`, `llm.card.provider`.
- Decide: 인터뷰·Card 생성 구현 직전에 시연 시나리오 하나를 같은 프롬프트로 Gemma와 fallback에 돌려 JSON 유효율, TRACE 필드 충족, 한국어 품질을 비교하고 라우팅을 확정한다.

### 흐름

- Interview: 턴마다 `{다음 질문, 추출한 TRACE 요소, 비어 있는 요소}` JSON을 받는다.
- End: 서버 룰(TRACE 완성도 체크리스트)이 종료를 판단한다. LLM이 종료를 결정하지 않는다.
- Card: 종료 시 TRACE Card를 1회 생성한다.

### 실패 처리

- Validate: 응답은 항상 스키마로 검증한다.
- Retry: 검증·호출에 실패하면 같은 Provider로 1회 재시도하고, 그다음 fallback으로 넘긴다.
- Exposure: 응답한 Provider는 로그에만 남기고 화면에는 노출하지 않는다.
- Parser: 응답 파서는 thought 블록이 있든 없든 견뎌야 한다(Gemma E4B는 thinking을 끄면 빈 thought 블록이 생기지 않는다).
- Error: 모든 Provider가 실패하면 `CustomException`으로 올린다. 코드는 해당 이슈에서 `ErrorCode`에 추가한다.

### 호출 예산

- Resource: GPU 자원이 작다. 호출은 작고 직렬로 하며 병렬 호출은 하지 않는다.
- Input: 인터뷰 턴은 누적 TRACE 상태 요약 + 직전 답변 + 남은 Gap 1~2개만, Card 생성은 TRACE 상태 요약 + Evidence 발췌만, 후임자 호출은 Card 요약 필드만 보낸다.
- Limit: 타임아웃과 출력 토큰 상한을 둔다.
- Target: 인터뷰 1건 4~6회, 후임자 상담 1건 2회.

### 데이터

- Synthetic: 외부 클라우드 fallback에는 합성 데이터만 보낸다. 실제 회사·인물 정보를 보내지 않는다.

### 미정 (Still open)

- SUH AI 연동 정보(엔드포인트·인증·요청 형식)는 아직 정해지지 않았다. 확정 전에는 어댑터를 임의로 구현하지 않고 사용자에게 묻는다. 전체 API 엔드포인트와 함께 한 이슈에서 정리한다.

## 현재 코드 상태

- `llm` 도메인 코드는 아직 없다.
