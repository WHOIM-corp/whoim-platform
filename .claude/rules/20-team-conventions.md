# Team Conventions

## Naming

- 식별자는 영어이고 약어는 쓰지 않는다(`id`, `url`, `dto`만 허용). 주석은 한국어로 "왜"가 필요한 곳에만 쓴다. 들여쓰기는 2칸(server).
- URL: `/api/<복수형-kebab-case>`, 버전 접두사 없음, 경로 변수도 kebab-case. 인증은 `/api/auth/...`.
- DTO: `XRequest`/`XResponse`, 파일 하나에 DTO 하나. 테이블은 소문자 snake_case 단수.
- web: 컴포넌트 PascalCase, 훅은 `use` 접두사.

## Code style

- 설정값은 `@Value` 대신 `@ConfigurationProperties`로 바인딩한다.
- DTO는 기본 `record`, 필요하면 `class`(Lombok 허용). 응답 DTO는 정적 팩토리 `from(...)`. 검증 메시지는 한국어.
- Lombok: `@Data` 금지. `@Getter`, `@Setter`, `@Slf4j`, `@RequiredArgsConstructor`는 허용하고 필드 주입은 쓰지 않는다. 엔티티는 `@NoArgsConstructor(access = PROTECTED)`와 상태·ID를 받지 않는 private 생성자의 `@Builder`.
- 에러: `throw new CustomException(ErrorCode.X)`. 도메인별 예외 클래스를 만들지 않는다. 응답 본문은 `{ errorCode, errorMessage }` 두 필드이고 HTTP 상태는 상태 줄에만 둔다. 성공 응답에는 envelope(`ApiResponse<T>`)를 쓰지 않고, 목록은 페이지네이션 없이 전체를 반환한다.
- 로깅: `@Slf4j`. 예상된 4xx는 WARN, 예상 못 한 예외는 ERROR와 스택이다. `System.out`·`printStackTrace` 금지. 민감정보 마스킹은 쓰지 않는다(사용자 결정). 단 secret 설정값은 로그와 문서에 남기지 않는다.
- 의존성: Lombok, starter-validation, springdoc, jjwt, Flyway, 로깅 라이브러리는 사전 승인이다. 그 외 추가는 사용자 확인 후.
- 영속성: 엔티티는 `BaseEntity` 상속, `UUID id`, `@ManyToOne(fetch = LAZY)`만 쓴다(`@OneToMany`·cascade 금지). enum은 `@Enumerated(STRING)`이고 DB는 `TEXT` + `CHECK`. 상태 변경은 이름 있는 도메인 메서드로 하고 전이 규칙은 상태 enum의 `canTransitionTo`에 둔다. 위반은 `STATE_CONFLICT`(409). hard delete만 쓴다.
- Flyway: SQL이 스키마의 원본이다(`ddl-auto: validate`). 파일명 `V<yyyyMMdd>_<HHmmss>__<설명>.sql`(KST). 적용된 파일은 수정하지 않고 변경은 새 파일로 추가한다. DB 초기화는 사용자가 한다.

## Responsibility separation

- Controller는 Service만 호출한다. 비즈니스 로직과 소유 검사는 Service에 둔다. `@Transactional`은 클래스가 아닌 메서드에 붙이고 조회는 `readOnly = true`다. 서비스는 도메인당 하나를 기본으로 한다.
- 권한 선언은 Controller에 `@PreAuthorize("hasAuthority('POSITION_...')")`로 한다.
- web: feature 단위로 나누고 서버 상태는 TanStack Query, 클라이언트 상태는 Zustand(도입 시). 화면은 `errorMessage`를 보여주고 분기는 `errorCode`로 한다.
- web: UI 언어는 한국어, 데스크톱(발표 화면) 우선. UI 작업 전에 HTML 정적 mock을 사용자와 합의한 뒤 구현한다. 접근성 전용 작업은 하지 않고 시맨틱 HTML은 평소처럼 쓴다. `any`와 `strict: false`로 우회하지 않는다.

## Web rules

- 단일 책임: 파일 하나에 컴포넌트 하나, 컴포넌트 하나에 역할 하나. 데이터 호출·상태·화면 표시를 한 컴포넌트에 섞지 않고 훅과 표시 컴포넌트로 나눈다. 헤더·사이드바·푸터 같은 영역도 역할별 컴포넌트와 파일로 분리한다.
- 폴더: `src/app`(진입·라우터·프로바이더), `src/layouts`(창·사이드바·툴바 등 화면 틀), `src/features/<name>/{api,components,hooks,model,pages}`, `src/shared/{api,components,hooks,lib,styles}`. 컴포넌트마다 폴더를 두고 스타일은 같은 폴더의 `*.module.scss`로 둔다.
- 아이콘: 코드 안에 SVG를 직접 작성하지 않는다. 아이콘 라이브러리(`lucide-react`)만 쓰고, 필요한 아이콘이 라이브러리에 없으면 사용자에게 요청한다.
- 모양: 웹으로 구현하지만 macOS 데스크톱 앱 느낌(떠 있는 창, 유리 질감의 사이드바·툴바, 액체 유리 선택 표시)을 유지한다. 폰트는 Pretendard 하나, 단위는 `rem`, 포인트 컬러 없이 색은 상태 표시에만 쓴다.
- 유지보수: 색·간격·경로·저장소 키 같은 값을 코드에 흩뿌리지 않고 토큰과 상수 한 곳에서 정의한다. 이름은 역할이 드러나게 짓고, 같은 코드를 두 번 쓰지 않는다.
- 오류 표시: 토스트를 쓰지 않고 화면 안의 `ErrorNotice`로 보여준다.

## Review expectations

- 확인: 요청 범위 밖 수정이 없는가, 상태 전이가 enum 규칙을 따르는가, 근거(DIRECT/RECORDED)를 서버가 검증하는가, 비밀값이 노출되지 않았는가, 규칙·이슈 정정이 반영됐는가.
- block 기준: 비밀값 노출, 제외 표 항목 구현, 적용된 Flyway 수정, `PUBLISHED` 데이터를 바꾸는 경로.
