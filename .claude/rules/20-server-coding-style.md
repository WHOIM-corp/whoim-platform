---
paths:
  - "server/**"
---

# Server Coding Style

## Basics

- Indent: 들여쓰기는 2칸이다. 포매터(Spotless 등)는 없다.
- Comment: 한국어로 쓰고, "왜"가 필요한 곳에만 쓴다.
- Naming: 식별자는 영어다. 약어는 쓰지 않는다(`id`, `url`, `dto`만 허용).
- Configuration: 설정값은 `@Value` 대신 `@ConfigurationProperties`로 바인딩한다.
- Dependency: Lombok, starter-validation, springdoc, jjwt, Flyway, 로깅 라이브러리(`com.chuseok22:logging:0.0.9`)는 사전 승인이다. 그 외 의존성은 사용자 확인 후 추가한다. Testcontainers와 H2는 쓰지 않는다.

## DTO

- Type: `record`를 쓴다. 상속이나 역직렬화 제약처럼 `record`가 어려울 때만 `class`를 허용한다.
- File: 파일 하나에 DTO 하나를 둔다.
- Location: 요청은 `application.dto.request`, 응답은 `application.dto.response`, 내부 전달용은 `application.dto.data`다.
- Name: `XRequest`, `XResponse`. `dto.data`는 이 이름 규칙을 따르지 않는다.
- Factory: 응답 DTO는 정적 팩토리 `from(...)`으로 만든다. MapStruct는 쓰지 않는다.
- Entity: 엔티티를 API 응답이나 외부로 노출하지 않는다.
- Exception: `ErrorResponse`는 `application.dto`가 아니라 `common.application.exception`에 둔다(의도된 예외, `40-server-api-and-error.md`).
- Validation: 검증 어노테이션의 `message`는 한국어로 쓴다. 첫 오류 메시지가 그대로 응답된다.

```java
public record MemberResponse(UUID id, String email) {

  public static MemberResponse from(Member member) {
    return new MemberResponse(member.getId(), member.getEmail());
  }
}
```

## Lombok

- Forbidden: `@Data`는 쓰지 않는다.
- Allowed: `@Getter`, `@Setter`, `@Slf4j`, `@RequiredArgsConstructor`.
- Injection: 생성자 주입은 `@RequiredArgsConstructor`로 한다. 필드 주입은 쓰지 않는다.
- Entity: `@NoArgsConstructor(access = PROTECTED)`와, 상태·ID를 받지 않는 private 생성자에 붙인 `@Builder`로 만든다(상세는 `30-server-persistence.md`).

## Service and transaction

- Service: 도메인당 하나를 기본으로 한다. 비대해지면 `XQueryService`와 `XCommandService`로 나눈다.
- Transaction: `@Transactional`은 클래스가 아닌 메서드 단위로 붙인다. 조회 메서드는 `readOnly = true`다.
- Controller: Service만 호출한다. Repository를 직접 호출하지 않는다.
- Mutation: 가능하면 새 객체를 만들어 반환한다. 엔티티 상태 변경은 이름 있는 도메인 메서드로만 한다.

## Time

- Store: 저장과 로직은 `Instant`다.
- Now: `Clock` 빈을 주입해 `Instant.now(clock)`만 쓴다. `Instant.now()`, `LocalDateTime.now()`를 직접 호출하지 않는다.
- Response: 응답 DTO는 `SeoulTime`으로 변환한 `OffsetDateTime`(`+09:00`)을 쓰고, `Instant`를 응답에 그대로 노출하지 않는다.
- Caution: `spring.jackson.time-zone: Asia/Seoul`은 `Instant`에 영향이 없고(항상 `Z`), `OffsetDateTime`은 컨텍스트 시간대에 따라 오프셋이 재조정될 수 있다. 변환은 `SeoulTime`으로 명시하고 직렬화 결과는 테스트로 확인한다.

## Logging

- Logger: `@Slf4j`를 쓴다.
- Monitoring: Controller 메서드 전체에 `@LogMonitoring`을 적용한다(`com.chuseok22:logging`, 개인 Nexus에서 읽기 공개라 CI 인증이 필요 없다).
- Sensitive: 민감정보 로깅에 제한을 두지 않는다(사용자 결정). 단 secret 설정값은 로그와 문서에 남기지 않는다.
- Level: 예상된 4xx는 WARN, 예상 못 한 예외는 ERROR와 스택이다(`40-server-api-and-error.md`).
- Forbidden: `System.out`, `printStackTrace`를 쓰지 않는다.

## Verification needed

- 로깅 라이브러리: `com.chuseok22:logging:0.0.9`의 Boot 4 호환과 AOP 동작은 도입 이슈에서 확인한다.
