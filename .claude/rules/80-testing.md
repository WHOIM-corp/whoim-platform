# Testing

## Policy override

- 전역 규칙("TDD 필수, 커버리지 80%", unit/integration/E2E 전부 필수)은 이 프로젝트에서 적용하지 않고 이 문서를 따른다.
- Reason: 메이커톤 일정상 속도가 중요하며, 본선은 Hero Flow 하나를 완성하는 방향이다.

## What to test

- 대상: 필요한 경우에만 쓴다. 커버리지 게이트는 없다.
- Server unit: 복잡한 상태 변경(Card/Package 상태 전이, TRACE 완성도 판단 등)만 단위 테스트한다(mock 또는 순수 단위).
- Server common: 공통 로직(예외 처리기, 시각 변환·직렬화)은 작은 테스트를 둔다.
- Server slice: 예외 처리기·직렬화는 `@WebMvcTest(controllers = …)` + `@AutoConfigureMockMvc(addFilters = false)` + `@ActiveProfiles("test")` 슬라이스(DB 불필요) 또는 `MockMvcBuilders.standaloneSetup`을 쓴다. 기본 프로필이 `prod`이므로 `@ActiveProfiles("test")`를 빼지 않는다.
- Not used: 통합 테스트, Testcontainers, H2는 쓰지 않는다. DB가 필요한 `@SpringBootTest`는 새로 만들지 않는다.
- TDD: 사용자가 요청한 경우에만 TDD(RED → GREEN → REFACTOR)로 진행한다.

## Current state

- Server: `ServerApplicationTests#contextLoads`(`@SpringBootTest`)는 datasource가 필요하다. dev 프로필(`SPRING_PROFILES_ACTIVE=dev`)과 사용자가 관리하는 `application-dev.yml` 없이는 실패한다.
- Server tests(DB 불필요): `SeoulTimeTest`, `GlobalExceptionHandlerTest`(`@WebMvcTest` 슬라이스), `AuthSecurityTest`(`@WebMvcTest` 보안·로그인 슬라이스, 필터 적용·고정 `Clock`·`MemberRepository` mock), `JwtProviderTest`, `JwtPropertiesTest`(64바이트 키 검증), `MemberSeedValidatorTest`, `MemberSeedRunnerTest`(순수 Mockito)가 있다. 이슈 #4에서 `LlmServiceTest`(파서·검증, mock), `SuhAiProviderTest`(`MockRestServiceServer`), `SuhAiPropertiesTest`(순수 단위), `SettingConfigServiceTest`(Repository mock)가 추가되었다. 서버 코드가 늘면 이 목록을 갱신한다.
- DB 필요: `ServerApplicationTests#contextLoads`는 `application-dev.yml`에 `llm.suh-ai.base-url`, `llm.suh-ai.api-key`, `llm.suh-ai.model` 세 키도 있어야 한다(없으면 `SuhAiProperties` 검증으로 실패한다).
- Security slice: 보안 슬라이스는 `jwt.secret-key`를 64바이트 이상 더미 값으로 `@TestPropertySource`에 지정한다(`JwtProperties` 검증 때문). `/actuator/health`는 슬라이스에서 401/403이 아님(공개)만 확인하며 200은 실기동에서 확인한다. 위 테스트 클래스 7개를 로컬에서 `./gradlew test --tests ...`로 실행해 55건 통과(실패 0, 에러 0, 스킵 0)를 확인했다(2026-10-15). 이슈 #4의 4개 클래스를 더한 11개 클래스를 로컬에서 실행해 90건 통과(실패 0, 에러 0, 스킵 0)를 확인했다(2026-10-16).
- Web: `test` 스크립트와 vitest가 없다. `package.json`의 scripts는 `dev`, `build`, `lint`, `preview`이다.

## CI does not prove tests pass

- Server CI(`PROJECT-SPRING-CI.yml`)는 `compileJava compileTestJava`와 `build -x test`만 실행한다. 테스트를 실행하지 않는다.
- Web CI(`PROJECT-REACT-CI.yaml`)는 `package.json`에 `test` 스크립트가 있을 때만 `CI=true npm test`를 실행하며, 실패하면 build 단계는 실행되지 않는다. `npm run lint`는 CI에서 실행하지 않는다(로컬 전용).
- 따라서 CI 통과는 테스트 통과가 아니다. 테스트를 돌렸다고 말하려면 로컬에서 직접 실행한 결과가 있어야 한다.

## Adding tests

- Server: JUnit Platform(`useJUnitPlatform()`)과 Boot 4의 분할 test starter(`starter-webmvc-test` 등)를 쓴다. 새 테스트 의존성은 사용자 확인 후 추가한다.
- Web: vitest를 도입하면 `test` 스크립트는 watch 없는 `vitest run`으로 둔다(CI가 그대로 실행하고, 실패 시 build가 막힌다).
- 실패 시 구현을 고친다. 테스트가 틀린 경우에만 테스트를 바꾼다.

## Commands

- Server: `cd server && ./gradlew test --tests '<클래스>'`(DB가 필요한 테스트는 `SPRING_PROFILES_ACTIVE=dev` 필요), 빌드는 `./gradlew clean build -x test`
- Web: `cd web && npm run lint && npm run build`

## Evidence

- 작업을 완료했다고 보고할 때는 실행한 명령과 실제 결과를 함께 적는다. 실행하지 못한 검증은 못 했다고 명시한다.
