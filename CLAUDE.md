# CLAUDE.md

이 파일은 진입점이며, 상세 규칙은 `.claude/rules/*.md`에 있습니다. 규칙은 모든 작업에 적용되는 것만 얇게 둡니다. 기능 상세는 각 이슈 구현 전 브레인스토밍에서 정합니다. 작업 시작 전 아래 파일들을 읽고 이해한 뒤 진행하세요.

- `00-project-overview.md`: 서비스 목적, 문서 우선순위, 스택, 명령, 구현 범위(P0~P2), 기획서 대비 제외 표, 제약, 변경 정책
- `10-architecture-and-boundaries.md`: 도메인 표, 의존 규칙, LLM 호출 원칙, 패키지 구조
- `20-team-conventions.md`: 네이밍, 코드 스타일, 에러·응답 규약, 영속성·Flyway, 책임 분리, web 규약
- `30-testing-and-verification.md`: 테스트 정책(전역 TDD·80% 규칙을 대체), 명령, 증거
- `40-delivery-and-review.md`: git 권한, main push = 배포·릴리즈, 보고서·PR 형식, 비밀·환경 파일, CI/CD

## 핵심 요약 (전체 내용은 위 rules 파일 참고)

- **서비스**: WHO I'M(후임). 시니어의 암묵적 판단을 AI 인터뷰로 추출해 TRACE Card로 만드는 지식 승계 서비스
- **구조**: 모노레포. `server/`(Spring Boot 4.1.1, Java 21, Gradle Groovy), `web/`(React 19, Vite 8, TypeScript, npm)
- **명령**: `cd server && ./gradlew clean build -x test`, `cd web && npm run lint && npm run build`
- **원칙**: 지정되지 않았거나 불확실하면 추측하지 않고 사용자에게 묻는다
- **절대 금지**:
  - 사용자 지시 없는 commit/push/PR/브랜치 생성 (`main` push는 곧 배포·릴리즈)
  - `git add -A`, `git add .` (force 계열 금지는 전역 규칙 참조)
  - 확정 범위 밖(`00-project-overview.md`의 제외 표)과 미결정 사항 임의 구현 (미결정은 해당 이슈의 브레인스토밍에서 정함)
  - mock 디자인을 사용자와 합의하기 전 UI 구현
  - `application-*.yml`, `web/.env.*` 무승인 수정, 값 노출, example 파일 생성, `.idea/` 접근
  - DB 초기화·drop·truncate·볼륨 삭제 (필요하면 원인과 절차만 안내)
  - project-auto-wizard 재실행, `.github/scripts/*`·`.github/.wizard/baseline.json`·`version.yml` 수정
  - `[skip ci]` 커밋 메시지, `v*` 태그 push, `gh workflow run`, `gh release create`, 서버 직접 배포
  - BE·FE 소스의 패키지·디렉터리명 `docs`/`issue`/`report` (`.gitignore`에 걸려 소스가 추적에서 빠짐). 루트 `docs/`는 예외로, 추적하지 않는 로컬 작업 공간이다(superpowers spec·plan 저장용)
  - `@Data`, `@OneToMany`, JPA cascade, 이미 적용된 Flyway 파일 수정
  - `ADMIN`에 업무 권한 자동 부여
  - 요청 범위 밖 수정

## 브레인스토밍 시작 절차

- 기준: 기획서(`docs/WHO_IM_Product_Origin.md`)가 origin이다. 충돌하면 기획서 > 규칙(규약·범위) > 최신 이슈 본문 순으로 따른다.
- 시작할 때 한 번만 읽는다: 기획서의 관련 절, 다루는 이슈와 직접 의존 이슈, 관련 규칙 1~2개. 읽은 내용은 spec(`docs/superpowers/specs/`) 맨 위에 10줄 이내 "시작 컨텍스트"로 요약한다.
- 이후에는 spec의 결정 로그가 기준이다. 이슈·규칙의 낡은 내용과 충돌하면 결정을 따르고 "정정 목록"에 모은다. 범위가 달라지는 충돌만 사용자에게 묻는다.
- 다시 읽어야 하면 무엇을 왜 읽는지 먼저 말한다. 브레인스토밍마다 이슈를 고치지 않고, 확정 후 한 번에 제안한다.
