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
- Signup: 회원가입은 없다. 로그인 식별자는 `email`이다.
- ADMIN: position이 없어 업무 API는 전부 거부한다. 자동 통과 규칙을 두지 않는다.

### 시드 계정

- Source: `application-*.yml`의 `seed.members` 목록(`email`, `password`, `name`, `role`, `position`). 시연 계정 4개(전임자, 후임자, 승인자, 관리자)다(차이점: BB는 `seed.member` 단일).
- Run: 서버 기동 시 `ApplicationRunner`(`member.infrastructure`)가 생성한다. 이메일 기준 멱등이고 비밀번호는 BCrypt로 저장한다.
- Validate: 형식 검증에 실패하면 기동을 실패시킨다. 값은 문서에 쓰지 않고 키 이름만 적는다.

### JWT

- Token: Access Token만 쓴다. 리프레시 토큰·서버 로그아웃 없음(클라이언트가 토큰을 폐기, 차이점: BB는 RT 있음).
- Impl: jjwt(`jjwt-api`+`jjwt-impl`+`jjwt-jackson`, 사전 승인) 기반 커스텀 `JwtProvider`와 `JwtAuthenticationFilter`. 서명은 HS512 고정.
- Claims: `sub`(memberId), `role`, `position`(ADMIN은 생략), `iss`, `iat`, `exp`.
- Config: `JwtProperties`(`@ConfigurationProperties(prefix = "jwt")`)로 주입한다. 키: `jwt.secret-key`, `jwt.access-exp-millis`, `jwt.issuer`.
- Delivery: `Authorization: Bearer` 헤더, 만료는 시연 중 끊기지 않게 넉넉히 둔다.

### 권한

- Authorities: 필터가 `ROLE_USER`/`ROLE_ADMIN`과 `POSITION_PREDECESSOR`/`POSITION_SUCCESSOR`/`POSITION_REVIEWER`로 변환한다.
- Declare: `@EnableMethodSecurity`를 켜고 Controller에 `@PreAuthorize("hasAuthority('POSITION_REVIEWER')")`로 선언한다.
- Ownership: 소유 검사는 서비스에서 한다. 전임자는 `handover_package.predecessor_id`가 자기, 후임자는 `successor_id`가 자기이고 PUBLISHED Package의 APPROVED Card만, 승인자는 모든 Package의 DRAFT 제외 Card.
- 권한 표: 전임자=인터뷰·필드 수정·Confirm / 승인자=Approve·Request Revision·Reject·Publish / 후임자=상담 / 관리자=아직 기능 없음.

### 보안 설정

- Paths: 공개 경로는 `SecurityPaths`(`auth.infrastructure.config`)의 static 상수로 관리한다. 공개: `/api/auth/login`, `/docs/swagger`, `/docs/swagger-ui/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`(CD 헬스체크), `/error`. 그 외는 인증 필요.
- CORS: 오리진은 `CorsOrigins` 상수 클래스로 관리하고 yml로 빼지 않는다. `http://localhost:5173`, `https://www.whoim.chuseok22.com`. `allowCredentials(true)`면 `*` 금지.
- Handler: 401/403도 `ErrorResponse`로 응답한다. `AuthenticationEntryPoint`/`AccessDeniedHandler`를 두고 Spring이 설정한 `ObjectMapper`를 주입한다(이슈 #2).
- Login: 실패는 이메일 존재 여부를 구분하지 않고 `INVALID_CREDENTIALS` 하나다.
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

- `SecurityPaths`(공개 경로 상수: actuator health, Swagger, `/error`)가 있고 `config/SecurityConfig`가 이를 사용한다. `SecurityConfig`는 아직 기본 `formLogin`+`httpBasic`이며 JWT·`CorsOrigins`·`/api/auth/login` 공개·401/403 처리는 이슈 #2에서 구현한다.
