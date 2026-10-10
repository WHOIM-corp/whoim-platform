# Testing

## Policy override

- 전역 규칙("TDD 필수, 커버리지 80%", unit/integration/E2E 전부 필수)은 이 프로젝트에서 적용하지 않고 이 문서를 따른다.
- Reason: 메이커톤 일정상 속도가 중요하며, 본선은 Hero Flow 하나를 완성하는 방향이다.

## What to test

- 대상: 핵심 도메인 로직만 필요할 때 테스트를 작성한다.
- Coverage: 커버리지 게이트를 두지 않는다.
- TDD: 사용자가 요청한 경우에만 TDD(RED → GREEN → REFACTOR)로 진행한다.

## Current state

- Server: 테스트는 `ServerApplicationTests#contextLoads` 하나(`@SpringBootTest`)이며 datasource가 필요하다. `application-dev.yml`이 비어 있거나 dev 프로필 없이 실행하면 실패한다.
- Web: `test` 스크립트와 vitest가 없다. `package.json`의 scripts는 `dev`, `build`, `lint`, `preview`이다.

## CI does not prove tests pass

- Server CI(`PROJECT-SPRING-CI.yml`)는 `compileJava compileTestJava`와 `build -x test`만 실행한다. 테스트를 실행하지 않는다.
- Web CI(`PROJECT-REACT-CI.yaml`)는 `package.json`에 `test` 스크립트가 있을 때만 `CI=true npm test`를 실행하며, 실패하면 build 단계는 실행되지 않는다. `npm run lint`는 CI에서 실행하지 않는다(로컬 전용).
- 따라서 CI 통과는 테스트 통과가 아니다. 테스트를 돌렸다고 말하려면 로컬에서 직접 실행한 결과가 있어야 한다.

## Adding tests

- Server: JUnit Platform(`useJUnitPlatform()`)과 Boot 4의 분할 test starter(`starter-data-jpa-test`, `starter-webmvc-test` 등)를 쓴다. Testcontainers·H2 등 새 의존성은 사용자 확인 후 추가한다.
- Web: vitest를 도입하면 `test` 스크립트는 watch 없는 `vitest run`으로 둔다(CI가 그대로 실행하고, 실패 시 build가 막힌다).
- 실패 시 구현을 고친다. 테스트가 틀린 경우에만 테스트를 바꾼다.

## Commands

- Server: `cd server && SPRING_PROFILES_ACTIVE=dev ./gradlew test` (dev yml 채운 뒤), 빌드는 `./gradlew clean build -x test`
- Web: `cd web && npm run lint && npm run build`

## Evidence

- 작업을 완료했다고 보고할 때는 실행한 명령과 실제 결과를 함께 적는다. 실행하지 못한 검증은 못 했다고 명시한다.
