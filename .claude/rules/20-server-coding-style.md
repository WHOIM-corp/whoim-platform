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

- Type: 기본은 `record`다. 상속이나 역직렬화 제약처럼 `record`가 어려울 때, 또는 Lombok을 쓰고 싶을 때는 `class`도 허용한다.
- Lombok: DTO에도 Lombok을 쓸 수 있다(`@Builder`, `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor` 등). `@Data`는 DTO에서도 쓰지 않는다. 사용자 결정(2026-10-16).
- File: 파일 하나에 DTO 하나를 둔다.
- Location: 요청은 `application.dto.request`, 응답은 `application.dto.response`, 내부 전달용은 `application.dto.data`다.
- Name: `XRequest`, `XResponse`. `dto.data`는 이 이름 규칙을 따르지 않는다.
- Factory: 응답 DTO는 정적 팩토리 `from(...)`으로 만든다. MapStruct는 쓰지 않는다.
- Entity: 엔티티를 API 응답이나 외부로 노출하지 않는다.
- Exception: `ErrorResponse`는 `application.dto`가 아니라 `common.application.exception`에 둔다(의도된 예외, `40-server-api-and-error.md`).
- Validation: 검증 어노테이션의 `message`는 한국어로 쓴다. 첫 오류 메시지가 그대로 응답된다.

```java
public record MemberResponse(UUID memberId, String username) {

  public static MemberResponse from(Member member) {
    return new MemberResponse(member.getId(), member.getUsername());
  }
}
```

## Lombok

- Forbidden: `@Data`는 쓰지 않는다.
- Allowed: `@Getter`, `@Setter`, `@Slf4j`, `@RequiredArgsConstructor`. DTO에는 `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`도 허용한다(`## DTO` 참고).
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
- Monitoring: Controller 메서드 전체에 `@LogMonitoring`을 적용한다(`com.chuseok22:logging`, 개인 Nexus에서 읽기 공개라 CI 인증이 필요 없다). 현재 `AuthController.login`에만 적용되어 있다(컨트롤러가 이것뿐).
- Sensitive: 민감정보 로깅에 제한을 두지 않는다(사용자 결정, 로그인 비밀번호·토큰 포함). 단 secret 설정값은 로그와 문서에 남기지 않는다.
- Level: 예상된 4xx는 WARN, 예상 못 한 예외는 ERROR와 스택이다(`40-server-api-and-error.md`).
- Forbidden: `System.out`, `printStackTrace`를 쓰지 않는다.

## Logging library (이슈 #2 확인 결과)

- Dependency: `com.chuseok22:logging:0.0.9`. 개인 Nexus 저장소(`https://nexus.chuseok22.com/repository/maven-releases/`)를 `server/build.gradle`의 `repositories`에 추가했다.
- Boot 4: 컴파일과 의존성 해석은 확인했다. 실제 로그 출력(AOP 동작)은 실기동 전이라 미검증이다.
- Transitive: 라이브러리 메타데이터는 `spring-boot-starter-web`·`aop` 3.5.7을 요구하지만 dependency-management 플러그인이 Boot 4.1.1 BOM으로 덮어써 4.1.1로 해석된다(캐시의 `spring-boot-starter-web/4.1.1` 확인).
- Jackson 2: 로깅 라이브러리가 내부에서 Jackson 2(`com.fasterxml`)를 쓰는데 이는 `jjwt-jackson` 전이 의존성에 기대고 있다. `jjwt-jackson`을 제거하면 `@LogMonitoring` 호출 시 `NoClassDefFoundError`가 날 수 있다.
- Sensitive keys: 마스킹은 쓰지 않는다(사용자 결정). 로그인 요청의 비밀번호와 응답의 토큰, `Authorization` 헤더가 `@LogMonitoring` 로그에 평문으로 남는다는 것을 알고 둔 상태다(라이브러리 기본값은 요청·응답 본문과 헤더를 로깅하고 `chuseok22.logging.sensitive-keys`가 비어 있다). 로그 접근 범위에 주의한다.
