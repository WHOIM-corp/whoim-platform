# Testing And Verification

## Test strategy

- 전역 규칙("TDD 필수, 커버리지 80%", unit·integration·E2E 전부 필수)은 이 프로젝트에서 적용하지 않는다. 메이커톤 일정상 속도가 중요하다. 커버리지 게이트는 없고, TDD는 사용자가 요청할 때만 한다.
- 대상: 복잡한 상태 변경(Card·Package 전이, TRACE 완성도 판단)과 공통 로직(예외 처리기, 시각 변환·직렬화)을 단위 테스트한다.
- 슬라이스: `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters = false)` + `@ActiveProfiles("test")`(DB 불필요). 기본 프로필이 `prod`이므로 `@ActiveProfiles("test")`를 빼지 않는다. 보안 슬라이스는 `jwt.secret-key`를 64바이트 이상 더미 값으로 `@TestPropertySource`에 지정한다.
- 미사용: 통합 테스트, Testcontainers, H2. DB가 필요한 `@SpringBootTest`는 새로 만들지 않는다.
- mock: 외부 LLM 호출은 Mockito 또는 `MockRestServiceServer`로 대체한다.

## Commands

- server: `cd server && ./gradlew test --tests '<클래스>'`(DB가 필요한 테스트는 `SPRING_PROFILES_ACTIVE=dev`), 빌드는 `./gradlew clean build -x test`
- web: `cd web && npm run lint && npm run build`
- 새 테스트 의존성은 사용자 확인 후 추가한다. web에 vitest를 도입하면 `test` 스크립트는 watch 없는 `vitest run`이다.

## Evidence

- 완료 보고에는 실행한 명령과 실제 결과를 함께 적는다. 실행하지 못한 검증은 못 했다고 명시한다.
- CI는 서버 테스트를 실행하지 않는다(`compileJava compileTestJava`와 `build -x test`만). 따라서 CI 통과는 테스트 통과가 아니고, 테스트를 돌렸다고 말하려면 로컬에서 직접 실행한 결과가 있어야 한다. web CI는 `package.json`에 `test` 스크립트가 있을 때만 `npm test`를 실행하고 `npm run lint`는 실행하지 않는다.

## Failure handling

- 실패하면 구현을 고친다. 테스트가 틀린 경우에만 테스트를 바꾼다.
