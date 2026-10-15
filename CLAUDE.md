# CLAUDE.md

이 파일은 진입점이며, 상세 규칙은 `.claude/rules/*.md`에 있습니다. 작업 시작 전 반드시 아래 파일들을 읽고 이해한 뒤 진행하세요.

- `00-project-overview.md`: 서비스 요약, 용어집, 일정, 스택, 주요 명령, 확정 구현 범위, 미정 항목
- `10-server-architecture.md`: 단일 모듈 DDD, 도메인 표, 패키지 구조, 의존 규칙
- `20-server-coding-style.md`: 스타일, DTO, Lombok, 서비스·트랜잭션, 시각, 로깅
- `30-server-persistence.md`: 엔티티, enum, 상태 전이, Flyway, Repository
- `40-server-api-and-error.md`: API 규약, 에러 처리, Swagger
- `50-server-auth-security.md`: 계정·role·position, JWT, 권한, 보안 설정, yml 취급
- `60-server-llm.md`: LLM Provider, 라우팅, 호출 예산, 실패 처리
- `70-web.md`: 웹 스택, 구조, 스타일, UI 작업 프로세스(mock 합의 선행)
- `80-testing.md`: 테스트 정책 (전역 TDD·80% 규칙을 대체)
- `90-delivery.md`: 커밋·push 권한, main push = 배포·릴리즈, 비밀·환경 파일, CI/CD, wizard 규칙

## 핵심 요약 (전체 내용은 위 rules 파일 참고)

- **서비스**: WHO I'M(후임). 시니어의 암묵적 판단을 AI 인터뷰로 추출해 TRACE Card로 만드는 지식 승계 서비스
- **구조**: 모노레포. `server/`(Spring Boot 4.1.1, Java 21, Gradle Groovy), `web/`(React 19, Vite 8, TypeScript, npm)
- **명령**: `cd server && ./gradlew clean build -x test`, `cd web && npm run lint && npm run build`
- **원칙**: 지정되지 않았거나 불확실하면 추측하지 않고 사용자에게 묻는다
- **절대 금지**:
  - 사용자 지시 없는 commit/push/PR/브랜치 생성 (`main` push는 곧 배포·릴리즈)
  - `git add -A`, `git add .` (force 계열 금지는 전역 규칙 참조)
  - 확정 범위 밖(제외 항목)과 `00-project-overview.md`의 "Still open" 영역 임의 구현
  - mock 디자인을 사용자와 합의하기 전 UI 구현
  - `application-*.yml`, `web/.env.*` 무승인 수정, 값 노출, example 파일 생성, `.idea/` 접근
  - DB 초기화·drop·truncate·볼륨 삭제 (필요하면 원인과 절차만 안내)
  - project-auto-wizard 재실행, `.github/scripts/*`·`.github/.wizard/baseline.json`·`version.yml` 수정
  - `[skip ci]` 커밋 메시지, `v*` 태그 push, `gh workflow run`, `gh release create`, 서버 직접 배포
  - 패키지·디렉터리명 `docs`/`issue`/`report` (`.gitignore`에 걸려 소스가 추적에서 빠짐)
  - `@Data`, `@OneToMany`, JPA cascade, 이미 적용된 Flyway 파일 수정
  - `ADMIN`에 업무 권한 자동 부여
  - 요청 범위 밖 수정
