---
paths:
  - "server/**"
---

# Server API And Error

URL, Controller, 응답, 에러 처리, Swagger 규칙이다. 인증·권한은 `50-server-auth-security.md`, DTO·검증 스타일은 `20-server-coding-style.md`를 따른다.

## 규약

### URL과 Controller

- URL: `/api/<복수형-kebab-case>`, 버전 접두사 없음, 경로 변수도 kebab-case(예: `/api/handover-packages/{package-id}`). 인증은 `/api/auth/...`.
- Docs: `XController`와 `XControllerDocs`(인터페이스)를 같은 패키지에 둔다. Swagger 어노테이션은 Docs에만 쓰고 Controller가 구현한다. 패키지명을 `docs`로 만들지 않는다(`.gitignore`가 `docs`를 무시).
- Schema: DTO 필드 설명·예시는 `@Schema(description, example)`로 DTO에 적고, 실패 케이스는 `@Operation`의 `description`에 errorCode와 함께 적는다.
- Call: Controller는 Service만 호출한다.
- Follow-up: 후임자 선택용 `GET /api/members?position=SUCCESSOR`(전임자만)는 member 도메인의 후속 이슈다.

### 응답

- Success: envelope 없이 DTO를 그대로, 필요하면 `ResponseEntity<Dto>`. `ApiResponse<T>` 같은 봉투 금지(전역 API Response Format 규칙의 의도된 예외).
- List: 목록은 페이지네이션 없이 전체를 반환한다.
- Error: 본문은 `{ "errorCode": "INVALID_REQUEST", "errorMessage": "잘못된 요청입니다" }` 두 필드뿐이다. `errorCode`는 enum 이름 문자열, HTTP 상태는 응답 상태 줄에만 담는다(차이점: BB는 code/message).

### 에러 클래스

- 위치: `common.application.exception`. `ErrorResponse`가 DTO 패키지가 아닌 여기 있는 것은 `20-server-coding-style.md` DTO 규칙의 의도된 예외다.
- `ErrorCode`: 중앙 enum 하나(`httpStatus`, 한국어 `message`). 도메인 코드는 같은 enum에 도메인별 주석 섹션으로 추가한다.
- `CustomException`: `(ErrorCode)`, `(ErrorCode, Throwable)` 두 생성자만. 메시지는 `ErrorCode`의 것을 쓴다.
- `ErrorResponse`: `record(errorCode, errorMessage)` + `of(ErrorCode)`, `of(ErrorCode, String)`. 다른 필드를 추가하지 않는다. Spring의 `org.springframework.web.ErrorResponse`를 import하지 않는다(이름 충돌).
- `GlobalExceptionHandler`: `@RestControllerAdvice` 하나가 모두 처리한다. `ResponseEntityExceptionHandler`는 상속하지 않는다(ProblemDetail 본문 방지).
- Throw: `throw new CustomException(ErrorCode.X)`. 메시지 문자열 직접 전달, 도메인별 예외 클래스 신설 금지.
- 공통 코드: `INVALID_REQUEST` 400, `MALFORMED_REQUEST_BODY` 400, `RESOURCE_NOT_FOUND` 404, `METHOD_NOT_ALLOWED` 405, `UNSUPPORTED_MEDIA_TYPE` 415, `STATE_CONFLICT` 409, `INTERNAL_SERVER_ERROR` 500. 인증 코드는 `50-server-auth-security.md`.

### 처리기 매핑

| 예외 | 응답 |
| --- | --- |
| `CustomException` | 해당 `ErrorCode`의 상태·코드 |
| `MethodArgumentNotValidException` | 400 `INVALID_REQUEST`, 첫 필드 오류의 `defaultMessage` |
| `HandlerMethodValidationException` | 400 `INVALID_REQUEST`, 첫 `getAllErrors()` 메시지 |
| `ConstraintViolationException` | 400 `INVALID_REQUEST`, 첫 위반 메시지 |
| `HttpMessageNotReadableException` | 400 `MALFORMED_REQUEST_BODY` |
| `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException` | 400 `INVALID_REQUEST` |
| `NoResourceFoundException` | 404 `RESOURCE_NOT_FOUND` |
| `HttpRequestMethodNotSupportedException` | 405 `METHOD_NOT_ALLOWED`, 지원 메서드가 있으면 `Allow` 헤더 |
| `HttpMediaTypeNotSupportedException` | 415 `UNSUPPORTED_MEDIA_TYPE` |
| 그 외 `Exception` | 500 `INTERNAL_SERVER_ERROR`, 내부 정보 비노출 |

- 추가: `ResponseStatusException`, 406 `HttpMediaTypeNotAcceptableException`, 413 `MaxUploadSizeExceededException` 등 표준 예외는 필요한 이슈에서 매핑을 추가한다.
- Security: `AccessDeniedException`/`AuthenticationException`은 캐치올이 삼키지 않는다. 401/403 `ErrorResponse`는 `50-server-auth-security.md`의 처리기가 맡는다.
- Order: 처리기는 하나뿐이라 `@Order`가 필요 없다. 다른 advice를 추가하면 `@Order(HIGHEST_PRECEDENCE)`와 캐치올 순서를 확인한다.
- Log: 예상된 `CustomException` 4xx는 WARN(스택 없음), 5xx와 예상 못 한 예외는 ERROR + 스택(원인은 서버 로그에만).

### Swagger

- Library: springdoc `3.1.1`을 `springdoc-openapi-starter-webmvc-ui`로 도입한다(#3, 사전 승인). 운영 on/off 없음.
- Config: `springdoc.*`(servers, media type, `swagger-ui.path: /docs/swagger`)는 `application.yml`에 이미 있다. 코드는 `OpenApiConfig`(`common.infrastructure.config`, bearerAuth HTTP bearer JWT + 전역 `SecurityRequirement`)만 둔다.
- Paths: `/docs/swagger`, `/docs/swagger-ui/**`, `/swagger-ui/**`, `/v3/api-docs/**`는 `SecurityPaths`로 공개한다. `swagger-ui.path`가 `/docs/swagger`라서 UI 리소스가 `/docs/swagger-ui/**`로 제공되므로 이 경로도 공개해야 한다(빠뜨리면 UI 리다이렉트가 401).
- Verify: Boot 4.1.1에서 `/v3/api-docs`, `/docs/swagger` 기동을 확인하고, 실패하면 BB의 `2.8.9`로 내린다.
- 검증 결과(2026-10-15): springdoc `3.1.1`이 Boot 4.1.1에서 기동했다(2.8.9로 내릴 필요 없음). dev 프로필로 확인: `/v3/api-docs` 200, `/docs/swagger`는 `/docs/swagger-ui/index.html`로 302 후 200. 기동 결과 `springdoc.servers` 블록은 문서에 반영되지 않았다(문서의 서버 URL이 접속 주소로 자동 생성됨). 서버 URL을 고정하려면 `OpenApiConfig`에서 `OpenAPI.servers`로 지정해야 하며 사용자 확인이 필요하다.

## 현재 코드 상태

- 구현됨(이슈 #3): `ErrorCode`, `CustomException`, `ErrorResponse`, `GlobalExceptionHandler`, `OpenApiConfig`. 컨트롤러는 아직 없다.
- 이슈 #2 전까지 `GlobalExceptionHandler`는 `AccessDeniedException`·`AuthenticationException`을 그대로 다시 던져 Security 필터 체인에 맡긴다. 인증되지 않은 요청의 없는 경로는 Security가 먼저 거부한다(API 요청은 401, 브라우저 `text/html` 요청은 `/login`으로 302). 그래서 404 `ErrorResponse`는 인증된 요청에서만 나온다.
