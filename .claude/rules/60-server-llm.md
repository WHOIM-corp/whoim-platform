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
- Layout: `application`(`dto.data.LlmRequest`·`LlmResponse`, `service.LLMProvider`·`LlmService`), `infrastructure`(`adapter.SuhAiProvider`, `config.SuhAiClientConfig`, `properties.SuhAiProperties`). `core.constant`(Provider 종류)는 Provider가 둘 이상 되는 시점에 만든다(아직 없음). Gemini 어댑터는 후속이다.
- Fake: fake Provider는 아직 만들지 않았다. 테스트는 Mockito mock과 `MockRestServiceServer`를 쓴다. 시연용 fake가 필요해지면 추가한다.
- Contract: `LlmRequest(system, prompt, responseSchema, temperature)`는 범용 계약이고, `system`·`responseSchema`·`temperature`가 null이면 Provider 기본값을 따른다. 도메인별 응답 스키마(TRACE 등)는 호출하는 도메인이 정한다. `LlmService#generate(request, responseType)`가 응답을 DTO로 파싱하고 Bean Validation으로 검증한다.

### Provider와 라우팅

- Default: SUH AI(Gemma 4 E4B, 전용 어댑터)를 기본으로, Gemini Flash-Lite급을 fallback으로 둔다.
- Config: 모델명·엔드포인트는 설정 값으로 두고 코드에 박지 않는다. API 키는 `application-*.yml`에만 두고 값은 문서에 쓰지 않는다. 키 이름은 추가 시 사용자에게 알린다.
- Keys: `application-*.yml`의 `llm.suh-ai.base-url`, `llm.suh-ai.api-key`, `llm.suh-ai.model` 세 개이며 `SuhAiProperties`(`@ConfigurationProperties(prefix = "llm.suh-ai")` record)가 바인딩한다. 하나라도 비어 있으면 `IllegalStateException`으로 기동이 실패한다(메시지에는 키 이름만, `toString`은 api-key를 마스킹). `base-url` 끝의 `/`는 제거한다.
- Dynamic: 모델과 temperature는 `setting_config`(`setting` 도메인, `SettingConfigService#getString`·`getDouble`)로 재정의할 수 있다. 문자열 값은 앞뒤 공백을 제거(trim)해 쓴다. 값이 없거나 공백이면 조용히 기본값을 쓰고, temperature 값이 숫자가 아닐 때만 WARN을 남기고 기본값을 쓴다(예외 아님). 쓰기 경로(관리자 화면·API)는 아직 없다.
- Precedence: 모델 = `setting_config("llm.suh-ai.model")` → yml `model`. temperature = `LlmRequest.temperature` → `setting_config("llm.suh-ai.temperature")` → 코드 기본값 `0.2`.
- Routing: 작업별 Provider를 설정으로 고른다. 키 `llm.interview.provider`, `llm.card.provider`는 아직 구현하지 않았다(후속, Gemini fallback 도입 시점에 함께 정한다).
- Decide: 인터뷰·Card 생성 구현 직전에 시연 시나리오 하나를 같은 프롬프트로 Gemma와 fallback에 돌려 JSON 유효율, TRACE 필드 충족, 한국어 품질을 비교하고 라우팅을 확정한다.

### 흐름

- Interview: 턴마다 `{다음 질문, 추출한 TRACE 요소, 비어 있는 요소}` JSON을 받는다.
- End: 서버 룰(TRACE 완성도 체크리스트)이 종료를 판단한다. LLM이 종료를 결정하지 않는다.
- Card: 종료 시 TRACE Card를 1회 생성한다.

### 실패 처리

- Validate: 응답은 항상 스키마로 검증한다.
- Retry: 재시도하지 않는다. 호출은 직렬 단일 호출이고 1회 실패하면 바로 예외다. Provider 체인·fallback(Gemini)은 후속 이슈다.
- Exposure: 응답한 Provider는 로그에만 남기고 화면에는 노출하지 않는다.
- Parser: 응답 파서는 thought 블록이 있든 없든 견뎌야 한다. `LlmService`는 `<think>…</think>` 블록과 코드펜스(```` ``` ````, ```` ```json ````)를 제거한 뒤 첫 `{`부터 마지막 `}`까지만 JSON으로 파싱한다.
- Error: 호출 실패(연결·읽기 타임아웃, HTTP 오류, `success`가 true가 아님, `error` 필드, 빈 `content`)와 응답 파싱·검증 실패는 모두 `CustomException(ErrorCode.LLM_UNAVAILABLE)`(503)이다. cause는 I/O 실패(`ResourceAccessException`: 연결·읽기 타임아웃)에만 남기고, 파싱·검증 실패, 역직렬화 실패, 그 밖의 `RestClientException`, `success`/`error`/빈 `content`/`null` 응답은 cause 없이 던진다(파서 메시지가 응답 본문 토큰을 담을 수 있다). 파싱 실패 로그에는 예외 클래스명만 남긴다. 응답 본문이 예외 메시지·cause·로그로 새지 않게 RestClient에 `defaultStatusHandler`를 둔다.
- Log: provider, model, metrics(토큰 수·소요 시간), 성공 여부만 남긴다. prompt, 응답 본문, API 키는 남기지 않는다.
- Transaction: LLM 호출은 느리므로(수십 초) 트랜잭션 밖에서 한다. 호출하는 서비스는 호출 전후로 트랜잭션을 나눈다.

### 호출 예산

- Resource: GPU 자원이 작다. 호출은 작고 직렬로 하며 병렬 호출은 하지 않는다.
- Input: 인터뷰 턴은 누적 TRACE 상태 요약 + 직전 답변 + 남은 Gap 1~2개만, Card 생성은 TRACE 상태 요약 + Evidence 발췌만, 후임자 호출은 Card 요약 필드만 보낸다.
- Limit: 타임아웃은 코드 상수로 연결 5초, 읽기 90초다(`SuhAiClientConfig`). 읽기 타임아웃은 JDK `HttpRequest.timeout`이라 응답 헤더 도착까지의 시간을 제한한다. 콜드 로드는 약 20초라 이 값을 줄이지 않는다. `auto_unload`는 항상 `false`다. 출력 토큰 상한은 아직 없다.
- Target: 인터뷰 1건 4~6회, 후임자 상담 1건 2회.

### 데이터

- Synthetic: 외부 클라우드 fallback에는 합성 데이터만 보낸다. 실제 회사·인물 정보를 보내지 않는다.

### SUH AI 호출 형식

- Request: `POST {base-url}/ollama/chat`, 인증 헤더 `X-API-Key`. 본문은 `{model, prompt, system?, temperature, auto_unload, format}`이고 `format`은 `responseSchema`가 있으면 그 JSON Schema 객체, 없으면 `"json"`이다.
- Response: `{content(JSON 문자열), metrics{eval_count, prompt_eval_count, total_duration_ms, ...}, model, success}`이고 오류는 본문의 `error`다. 미지 필드는 무시한다.
- HTTP client: Boot 자동구성 `RestClient.Builder`가 classpath에 없어 정적 `RestClient.builder()`와 `JdkClientHttpRequestFactory`를 쓴다(`suhAiRestClient` 빈).

### 미정 (Still open)

- Gemini fallback 어댑터, Provider 체인·작업별 라우팅(`llm.interview.provider`, `llm.card.provider`), 모델 목록 조회, `setting_config` 쓰기 경로(관리자), 프롬프트 키, 도메인별 응답 스키마는 아직 정해지지 않았다. 사용자 지시 없이 구현하지 않는다. 전체 API 엔드포인트 명세는 별도 이슈다.

## 현재 코드 상태

- 구현됨: `LlmRequest`, `LlmResponse`, `LLMProvider`, `LlmService`(파싱·검증), `SuhAiProvider`(SUH AI 단일 호출), `SuhAiClientConfig`, `SuhAiProperties`, `ErrorCode.LLM_UNAVAILABLE`. `setting` 도메인의 `SettingConfigService`를 모델·temperature 조회에 쓴다.
- 미검증: 실제 SUH AI 호출(dev 프로필)은 확인하지 못했다. 테스트는 mock 기반이다(`80-testing.md`).
