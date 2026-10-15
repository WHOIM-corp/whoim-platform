---
paths:
  - "server/**"
---

# Server Auth And Security

계정·JWT·권한·보안 설정 규칙이다. 에러 응답 형식은 `40-server-api-and-error.md`, member 테이블 제약은 `30-server-persistence.md`를 따른다.

## 규약

### 계정 모델

- role: `USER`/`ADMIN`. ADMIN은 나중에 쓰며 승인자와 다르다.
- position: `PREDECESSOR`(전임자)/`SUCCESSOR`(후임자)/`REVIEWER`(승인자), enum `Position`. ADMIN은 position이 없다(DB CHECK는 `30-server-persistence.md`).
- Signup: 회원가입은 없다. 로그인 식별자는 `username`이다(`email` 컬럼은 없다).
- ADMIN: position이 없어 업무 API는 전부 거부한다. 자동 통과 규칙을 두지 않는다.

### 시드 계정

- Source: `application-*.yml`의 `seed.members` 목록(`username`, `password`, `name`, `role`, `position`). 시연 계정 4개(전임자, 후임자, 승인자, 관리자)다. 값은 yml에만 있고 문서에 쓰지 않는다(차이점: BB는 `seed.member` 단일).
- Bind: `SeedProperties`(`member.infrastructure.properties`, `@ConfigurationProperties(prefix = "seed")`)로 바인딩한다. `seed.members`가 없으면 빈 목록이다.
- Run: 서버 기동 시 `MemberSeedRunner`(`ApplicationRunner`, `member.infrastructure.config`)가 생성한다. 비밀번호는 BCrypt로 저장한다.
- Idempotent: `username`이 이미 있으면 건너뛰고 기존 계정을 수정하지 않는다(비밀번호·role 변경 반영 안 됨). 목록이 비어 있으면 로그만 남기고 건너뛴다.
- Validate: 실행 전 `MemberSeedValidator`가 검증하고 실패하면 `IllegalStateException`으로 기동을 실패시킨다. 검사: 빈 항목·빈 `username`/`password`/`name`·`username` 중복·`role` 누락·ADMIN인데 position 있음·USER인데 position 없음. 메시지에는 인덱스와 `username`만 담고 비밀번호는 담지 않는다.

### JWT

- Token: Access Token만 쓴다. 리프레시 토큰·서버 로그아웃 없음(클라이언트가 토큰을 폐기, 차이점: BB는 RT 있음).
- Impl: jjwt(`jjwt-api`+`jjwt-impl`+`jjwt-jackson`, 사전 승인) 기반 커스텀 `JwtProvider`와 `JwtAuthenticationFilter`. 서명은 HS512 고정.
- Claims: `sub`(memberId), `username`, `role`, `position`(ADMIN은 생략), `iss`, `iat`, `exp`. 파싱은 서명(HS512만 허용)·issuer·만료를 검증하고, 만료는 `EXPIRED_TOKEN`, 그 외 실패는 `UNAUTHENTICATED`다. 시각은 `Clock` 빈을 따른다.
- Config: `JwtProperties`(`@ConfigurationProperties(prefix = "jwt")` record, `SecurityConfig`의 `@EnableConfigurationProperties`로 등록)로 주입한다. 키: `jwt.secret-key`, `jwt.access-exp-millis`, `jwt.issuer`. `refresh-exp-millis` 등 다른 키는 바인딩하지 않는다.
- Key check: `jwt.secret-key`가 비었거나 UTF-8 64바이트 미만이면 `JwtProperties` 생성 시 `IllegalStateException`으로 기동이 실패한다(값은 메시지에 없음).
- Login: `POST /api/auth/login` 응답은 `{ accessToken, member }`다(`40-server-api-and-error.md`). 로그인 서비스는 `LoginService`다.
- Delivery: `Authorization: Bearer` 헤더, 만료는 시연 중 끊기지 않게 넉넉히 둔다.

### 권한

- Principal: 인증 주체는 `AuthenticatedMember`(`memberId`, `username`, `role`, `position`, `auth.application.dto.data`)다. 컨트롤러는 `@AuthenticationPrincipal`로 받는다. 필터는 토큰 클레임만 신뢰하고 DB를 조회하지 않는다.
- Authorities: 필터가 `ROLE_USER`/`ROLE_ADMIN`과 `POSITION_PREDECESSOR`/`POSITION_SUCCESSOR`/`POSITION_REVIEWER`로 변환한다.
- Declare: `@EnableMethodSecurity`는 `SecurityConfig`에 켜져 있다. Controller에 `@PreAuthorize("hasAuthority('POSITION_REVIEWER')")`로 선언한다.
- Ownership: 소유 검사는 서비스에서 한다. 전임자는 `handover_package.predecessor_id`가 자기, 후임자는 `successor_id`가 자기이고 PUBLISHED Package의 APPROVED Card만, 승인자는 모든 Package의 DRAFT 제외 Card.
- 권한 표: 전임자=인터뷰·필드 수정·Confirm / 승인자=Approve·Request Revision·Reject·Publish / 후임자=상담 / 관리자=아직 기능 없음.

### 보안 설정

- Location: 보안 클래스(`SecurityConfig`, `JwtProvider`, `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`, `CorsOrigins`, `SecurityPaths`)는 모두 `auth.infrastructure.config`에 있다. `JwtProperties`만 `auth.infrastructure.properties`다.
- Chain: stateless, csrf·formLogin·httpBasic·logout 비활성, `anyRequest().authenticated()`. `JwtAuthenticationFilter`는 빈이 아니라 `SecurityConfig`에서 만들어 `UsernamePasswordAuthenticationFilter` 앞에 넣는다(이중 등록 방지). 비밀번호 인코더는 `BCryptPasswordEncoder` 빈이다.
- Paths: 공개 경로는 `SecurityPaths`(`auth.infrastructure.config`)의 static 상수로 관리한다. 공개: `/api/auth/login`, `/docs/swagger`, `/docs/swagger-ui/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`(CD 헬스체크), `/error`. 그 외는 인증 필요.
- CORS: 오리진은 `CorsOrigins` 상수 클래스로 관리하고 yml로 빼지 않는다. `http://localhost:5173`, `http://localhost:3000`, `https://www.whoim.chuseok22.com`. 허용 메서드 GET/POST/PUT/PATCH/DELETE/OPTIONS, 허용 헤더 `Authorization`/`Content-Type`. `allowCredentials(true)`면 `*` 금지.
- Handler: 401/403도 `ErrorResponse`로 응답한다. `JwtAuthenticationEntryPoint`/`JwtAccessDeniedHandler`가 Spring이 설정한 `ObjectMapper`(Jackson 3, `tools.jackson`)로 직렬화한다. 필터가 토큰 파싱에 실패하면 요청 속성에 `ErrorCode`를 남기고 entry point가 `EXPIRED_TOKEN`/`UNAUTHENTICATED`로 응답한다.
- Login: 실패는 아이디 존재 여부를 구분하지 않고 `INVALID_CREDENTIALS` 하나다.
- Actuator: 노출은 `health`뿐이다.

| ErrorCode | HTTP | 용도 |
| --- | --- | --- |
| `UNAUTHENTICATED` | 401 | 토큰 없음·잘못됨 |
| `EXPIRED_TOKEN` | 401 | AT 만료 |
| `INVALID_CREDENTIALS` | 401 | 로그인 실패 |
| `ACCESS_DENIED` | 403 | position 부족 |

### application-*.yml 취급

- Git: `application-dev.yml`/`application-prod.yml`은 gitignore다. 로컬 존재 여부와 내용은 사용자가 관리하고, prod는 CI가 `APPLICATION_PROD_YML`로 덮어쓴다.
- Read: 읽을 수 있으나 키 이름만 확인하고 값은 출력하지 않는다.
- Edit: 수정은 사용자 승인 후에만 한다(차이점: BB는 읽기 전용). 비밀값을 문서·코드에 쓰지 않는다.
- Profile: `application.yml`의 기본 프로필이 `prod`다. 로컬 실행은 `SPRING_PROFILES_ACTIVE=dev`, 테스트는 `@ActiveProfiles("test")`.

## 현재 코드 상태

- 구현됨(이슈 #2): JWT 발급·검증(`JwtProvider`), 필터, 401/403 처리기, `SecurityConfig`(CORS, 공개 경로, `@EnableMethodSecurity`), 로그인(`AuthController`, `LoginService`), 시드(`MemberSeedRunner`), `Member`. 구조는 위 규약과 같다.
- 컴파일(`./gradlew compileJava`)은 통과했다. 보안 동작은 `@WebMvcTest` 슬라이스 테스트로 확인하는 구성이다(`80-testing.md`).
- 미검증: 실제 DB와 dev 프로필로 기동해 시드 생성과 로그인을 확인하지 못했다. 두 yml에 `seed.members`가 없으면 시드는 건너뛴다(사용자 승인 후 yml 수정). 배포는 GitHub Secret `APPLICATION_PROD_YML`에 `seed.members`와 64바이트 이상 `jwt.secret-key`가 있어야 한다.
- 미검증: `@PreAuthorize` 기반 position 권한은 아직 쓰는 컨트롤러가 없다.
