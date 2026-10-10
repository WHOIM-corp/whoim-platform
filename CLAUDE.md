# CLAUDE.md

이 파일은 진입점이며, 상세 규칙은 `.claude/rules/*.md`에 있습니다. 작업 시작 전 반드시 아래 파일들을 읽고 이해한 뒤 진행하세요.

- `.claude/rules/00-project-overview.md`: 서비스 요약, 용어집, 일정, 스택, 주요 명령, 미확정 영역
- `.claude/rules/10-server.md`: 서버 패키지 구조, DTO·에러·시간·DB·설정 규칙
- `.claude/rules/20-web.md`: 웹 스택, 구조, 스타일, UI 작업 프로세스(mock 합의 선행)
- `.claude/rules/30-testing.md`: 테스트 정책 (전역 TDD·80% 규칙을 대체)
- `.claude/rules/40-git-and-delivery.md`: 커밋·push 권한, main push = 배포·릴리즈, 릴리즈 봇
- `.claude/rules/50-secrets-and-env.md`: 비밀 파일 취급, GitHub Secrets 목록
- `.claude/rules/60-ci-cd.md`: 설치된 워크플로우, Dockerfile, wizard 규칙

## 핵심 요약 (전체 내용은 위 rules 파일 참고)

- **서비스**: WHO I'M(후임). 시니어의 암묵적 판단을 AI 인터뷰로 추출해 TRACE Card로 만드는 지식 승계 서비스
- **구조**: 모노레포. `server/`(Spring Boot 4.1.1, Java 21, Gradle Groovy), `web/`(React 19, Vite 8, TypeScript, npm)
- **명령**: `cd server && ./gradlew clean build -x test`, `cd web && npm run lint && npm run build`
- **원칙**: 지정되지 않았거나 불확실하면 추측하지 않고 사용자에게 묻는다
- **절대 금지**:
  - 사용자 지시 없는 commit/push/PR/브랜치 생성 (`main` push는 곧 배포·릴리즈)
  - `git add -A`, `git add .` (force 계열 금지는 전역 규칙 참조)
  - 미확정 영역(구현 범위, AI/LLM, 개인정보, 인증·권한) 임의 구현
  - mock 디자인을 사용자와 합의하기 전 UI 구현
  - `application-*.yml`, `web/.env.*` 무승인 수정, 값 노출, example 파일 생성, `.idea/` 접근
  - DB 초기화·drop·truncate·볼륨 삭제 (필요하면 원인과 절차만 안내)
  - project-auto-wizard 재실행, `.github/scripts/*`·`.github/.wizard/baseline.json`·`version.yml` 수정
  - `[skip ci]` 커밋 메시지, `v*` 태그 push, `gh workflow run`, `gh release create`, 서버 직접 배포
  - 요청 범위 밖 수정
