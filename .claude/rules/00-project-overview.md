# Project Overview

## Purpose

- 서비스: WHO I'M(후임). 슬로건 "전임자의 경험을, 후임자의 판단으로."
- 목적: 시니어의 암묵적 판단을 AI로 발견·복원·검증해 근거가 연결된 TRACE Card로 만들고, 후임자가 실제 업무에서 쓰게 하는 기업·기관용 지식 승계 서비스다. 인수인계 문서나 RAG 챗봇이 아니라 Guided Decision Support다.
- 흐름(기획서): 업무 기록 분석(Gap 탐지) → 전임자 AI 인터뷰(Phronesis Interview, TRACE) → TRACE Card → 전임자 확인 + 승인자 승인 → Package 공개 → 후임자 상담.
- 사용자: 전임자(`PREDECESSOR`), 후임자(`SUCCESSOR`), 승인자(`REVIEWER`). `ADMIN`은 업무 권한이 없다.
- 맥락: 2026학년도 세종대 인공지능융합대학 메이커톤(개인 참가). 팀명은 코드와 문서에 넣지 않는다. 본선 11/4(수) 18:00, 영상과 현장 시연.
- Brand: 표기는 항상 `WHO I'M`. 내부 식별자(패키지, 컨테이너, 도메인)에만 `whoim`을 쓴다.

## Source of truth

- origin: `docs/WHO_IM_Product_Origin.md`(예선 기획서, 로컬 전용)가 최상위 기준이다. 충돌하면 기획서 > 규칙(규약·범위) > 최신 이슈 본문 순으로 따른다.
- 규칙은 얇게 유지한다. 모든 작업에 적용되는 것만 두고, 기능 상세는 각 이슈 구현 전 브레인스토밍에서 정해 이슈 본문에 반영한다. 스키마의 원본은 코드와 Flyway SQL이다.
- 루트 `docs/superpowers/`(spec, plan)는 일부러 추적하지 않는 로컬 작업 공간이다. 커밋하지 않고 `git add -f`도 쓰지 않는다. Obsidian은 구현 이력을 남기는 참고용이다.

## Primary Stack

- Language: Java 21(server), TypeScript(web)
- Framework: Spring Boot 4.1.1(Gradle Groovy, JPA, Spring Security, Flyway, Actuator), React 19.2 + Vite 8
- Database / storage: PostgreSQL
- LLM: 생성은 Gemini, DLP 탐지는 SUH AI(Gemma 4 E4B)이고 실패 시 Gemini로 넘어간다
- Infra / deployment: Docker(Docker Hub) + SSH 서버. API `https://api.whoim.chuseok22.com`, web `https://www.whoim.chuseok22.com`

## Important directories

- `server/`: Spring Boot, 루트 패키지 `com.chuseok22.whoim.server`
- `web/`: React
- `.github/`: 워크플로우, 루트 `version.yml`
- `.claude/rules/`: 이 규칙들
- `docs/`: 로컬 전용(기획서, spec, plan). 추적하지 않는다

## Main commands

- server run: `cd server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`
- server build: `cd server && ./gradlew clean build -x test`
- web: `cd web && npm ci`, `npm run dev`, `npm run lint`, `npm run build`
- Profile: 기본 프로필은 `prod`다. 로컬은 `SPRING_PROFILES_ACTIVE=dev`, 테스트는 `@ActiveProfiles("test")`

## Scope

- Goal: Hero Flow(Evidence-first → TRACE → Verified TRACE → Decision Reuse) 하나만 완성한다. 개발은 단독 약 3주다.
- P0: 데이터 모델, JWT 인증, 문서 업로드, Gap 탐지("분석 시작", Package당 1회), 인터뷰(3~5턴, 텍스트만) → Card 생성, 전임자 Confirm, 승인자 Approve·Reject·Package 생성·Publish, 후임자 상담.
- P1: Reject와 Card 이력 타임라인, DLP(Rule/Regex + AI 2차 탐지), 근거 자료 화면(파일 업로드와 연동 소스 읽기 전용 표시).
- P2: Gap 유형 일반화, Fail-Closed 시연 코드, UI polish.
- Data: 합성 데이터만 쓴다. 실제 회사·인물·내부 정보를 코드, 테스트, 스크린샷에 넣지 않는다. 합성 데이터와 시연 대본은 Claude가 초안을 쓰고 사용자가 검수한다.
- Accounts: 회원가입 없음. 시연 계정 4개(전임자, 후임자, 승인자, 관리자)를 `application-*.yml`의 `seed.members`로 서버 기동 시 만든다. 나머지 데이터는 앱 기능으로 입력한다.

### 기획서 대비 제외 표

| 항목 | 기획서 | 개발 단계 결정 | 사유 |
| --- | --- | --- | --- |
| 전임자의 Card 수정·보완 | 4-4: 잘못된 해석을 보완 | 제외. Confirm만 한다 | MVP 단순화 |
| 승인자의 수정 요청 루프 | 4-4: 회사 검수 | 제외. Approve·Reject만 한다 | MVP 단순화 |
| Reject된 판단의 재인터뷰 | 언급 없음 | 제외. Gap당 인터뷰 하나 | MVP 단순화 |
| 실제 Git·Jira·Wiki 연동 | 4-6: 근거 출처로 연동 | 제외. 문서 업로드만. 웹 근거 자료 화면에 "연동 소스" 읽기 전용 표시(모의, 서버 Connector 없음)는 P1 | 일정 |
| 판단 발생 시점부터 지속 축적 | 3-5 | 제외(Scheduler 없음) | 일정 |
| 온프레미스·로컬 AI DLP | 4-6: 전면 적용 | Rule/Regex + AI 2차 탐지(SUH AI, 실패 시 Gemini에는 Rule 마스킹 텍스트만). 시연 연출 제외 | 일정 |
| 관리자의 최종 공개 | 4-4: 관리자 | 승인자(`REVIEWER`)가 Approve와 Publish를 모두 한다 | 역할 단순화 |

- 그 밖의 제외: Enterprise TRACE Search, Versioning UI, 대시보드, 알림, 음성, SSO, 멀티테넌시, 시드 로더 프로그램, 벡터 DB, Claim 단위 Evidence, 사용자 관리 화면. 사용자 지시 없이 구현하지 않는다.

## Project-specific constraints

- 불확실하면 추측하지 않고 사용자에게 묻는다. 제외 표 항목과 미결정 사항은 사용자 지시 없이 구현하지 않고, 미결정은 해당 이슈의 브레인스토밍에서 정한다.
- 역할: `role`은 `USER`/`ADMIN`, `position`은 `PREDECESSOR`/`SUCCESSOR`/`REVIEWER`. `ADMIN`에 업무 권한을 자동 부여하지 않는다.
- 권한: 전임자 = 문서 업로드·분석 시작·인터뷰·Confirm. 승인자 = Package 생성·후임자 지정·Approve·Reject·Publish. 후임자 = 상담. 소유 검사는 서비스에서 한다.
- 근거 원칙: AI 추정은 공식 판단으로 저장하지 않는다. 근거는 인터뷰 발언(DIRECT)과 문서 범위(RECORDED)뿐이다. 후임자는 `PUBLISHED` Package의 `APPROVED` Card만 본다. `PUBLISHED` Package는 수정하지 않는다.
- UI: mock 디자인을 사용자와 합의하기 전에는 구현하지 않는다.
- 금지: `@Data`, `@OneToMany`, JPA cascade, Testcontainers, H2.
- 품질: 승인자 핵심은 정상·예외·상태 오류까지, 보조 기능은 단순하게 만든다.

## Change policy

- 요청 범위 밖은 수정하지 않는다. 적용된 Flyway 파일은 수정하지 않는다.
- `application-*.yml`, `web/.env.*`는 승인 없이 수정하지 않고 값을 노출하지 않는다. `.idea/`는 읽지도 수정하지도 않는다.
- CI가 관리하는 파일(`version.yml`, `server/build.gradle`·`web/package.json`의 `version`, `.github/scripts/*`, `.github/.wizard/baseline.json`)은 수정하지 않는다. `project-auto-wizard`는 사용자가 직접 실행한다.
- DB 초기화·drop·truncate·볼륨 삭제는 하지 않는다. 필요하면 원인과 절차만 안내한다.
