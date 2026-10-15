# Project Overview

## Service

- Name: WHO I'M(후임). 슬로건 "전임자의 경험을, 후임자의 판단으로."
- Purpose: 시니어의 암묵적 판단을 AI 인터뷰(Phronesis Interview, TRACE 프레임워크)로 추출해 근거(Evidence)가 연결된 TRACE Card로 만들고, 후임자가 실제 업무에서 쓰게 하는 기업·기관용 지식 승계 서비스다. 인수인계 문서나 RAG 챗봇이 아니라 Guided Decision Support다.
- Context: 2026학년도 세종대 인공지능융합대학 메이커톤 출품작(개인 참가). 팀명은 코드와 문서에 넣지 않는다. (사용자 확인, 2026-10-10 기준)
- Brand: 표기는 항상 `WHO I'M`(대문자, 공백, apostrophe). 내부 식별자(패키지, 컨테이너, 도메인)에만 `whoim`을 쓴다. `WHOIM`, `Whoim`은 쓰지 않는다.

## Glossary

- TRACE: Trigger, Root Cause, Assessment, Choice, Exception & Exposure
- TRACE Card: 근거가 연결된 판단 카드
- Phronesis Interview: 적응형 후속 질문 인터뷰
- Evidence: 인터뷰, Incident, Runbook, Jira 등 판단의 근거
- Handover Package: 검수 후 후임자에게 공개되는 묶음
- 2단계 검수: 전임자 확정 → 승인자 승인 → 후임자 공개. Capture와 Release는 다르다
- DLP: Fail-Closed 원칙
- role: 계정 종류 `USER`/`ADMIN`. `ADMIN`은 나중에 쓰며 승인자와 다르다
- position: 업무 위치 `PREDECESSOR`(전임자)/`SUCCESSOR`(후임자)/`REVIEWER`(승인자). `USER`만 가지며 `ADMIN`은 없다

## Schedule (사용자 확인, 2026-10-10 기준)

- 1차 예선 결과 10/14, 본선 11/4(수) 18:00. 본선은 제출한 기획안을 메이킹한 결과물이다.
- 본선 구현 범위는 아래 "Implementation scope"를 따른다.

## Structure and stack

- Monorepo: `server/`, `web/`, `.github/`(워크플로우), 루트 `version.yml`
- server: Spring Boot 4.1.1, Java 21, Gradle(Groovy), JPA + PostgreSQL, Spring Security, Actuator. 루트 패키지 `com.chuseok22.whoim.server`. Thymeleaf starter와 `thymeleaf-extras-springsecurity6`도 의존성에 포함(용도 미정).
- server libraries: Lombok, starter-validation, springdoc(Swagger 3.1.1)은 이슈 #3에서 도입했다. jjwt, Flyway, 로깅 라이브러리(`com.chuseok22:logging`)는 사전 승인 목록이며 각 이슈에서 도입하므로 `server/build.gradle`에 있는 것만 현재 의존성이다.
- web: React 19.2, Vite 8, TypeScript ~6.0, npm. CI는 Node 22.
- Deploy: Docker(Docker Hub) + SSH 서버(Synology 추정). API `https://api.whoim.chuseok22.com`, web `https://www.whoim.chuseok22.com` (도메인은 사용자 확인).

## Commands

- server run: `cd server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`
- server build: `cd server && ./gradlew clean build -x test`
- web: `cd web && npm ci`, `npm run dev`, `npm run build`, `npm run lint`
- Profile: `application.yml`의 기본 프로필은 `prod`다. 로컬 실행은 `SPRING_PROFILES_ACTIVE=dev`, 테스트는 `@ActiveProfiles("test")`로 지정한다.
- Config: `application-dev.yml`, `application-prod.yml`은 gitignore이며 내용은 사용자가 관리한다. prod는 CI가 `APPLICATION_PROD_YML`로 만든다(`90-delivery.md`).
- Caveat: dev 프로필과 DB 없이는 `bootRun`과 `ServerApplicationTests#contextLoads`가 동작하지 않는다.

## Implementation scope (사용자 확정, 2026-10-13 기준)

- Goal: Hero Flow(Evidence-first → TRACE → Verified TRACE → Decision Reuse) 하나만 완성한다. 본선(11/4)은 영상 + 현장 시연이고, 개발은 단독 약 3주다.
- P0: 데이터 모델과 상태 Enum, JWT 인증, 인터뷰(3~5턴, 텍스트만) → TRACE Card 생성, 전임자 리뷰(필드 수정 + Confirm), 승인자 핵심(Card 목록·상세, Approve, Request Revision, Package 진행률·READY·Publish), 후임자 Guided Decision Support(`PUBLISHED`+`APPROVED` Card만 → 역질문 → Card 제시, Settlement Batch 두 분기).
- P1: Evidence 업로드(`.md`·`.txt`)와 Gap Detection, 승인자 보조(Reject, 이력 타임라인), DLP(Rule/Regex).
- P2: Gap Detection 일반화, Fail-Closed 시연 코드, UI polish.
- Excluded: 실제 Connector, Scheduler, Enterprise TRACE Search, Versioning UI, 대시보드, 알림, 음성, SSO, 멀티테넌시, 시드 로더 프로그램(데이터 파일 적재), 벡터 DB, Claim 단위 Evidence, 사용자 관리 화면. 사용자 지시 없이 구현하지 않는다.
- Order: ① 모델·인증 골격 → ② 인터뷰·Card 생성·전임자 리뷰 → ③ 승인자 페이지 → ④ 후임자 Decision Support → ⑤ Evidence·Gap Detection → ⑥ DLP·Hard Rule 보강
- Quality: 승인자 핵심은 정상·예외·상태 오류까지 완성도를 높이고, 보조 기능은 단순하게 만든다.
- Data: 합성 데이터만 쓴다. 실제 회사·인물·내부 정보를 코드, 테스트, 스크린샷에 넣지 않는다. 합성 데이터와 시연 대본은 Claude가 초안을 쓰고 사용자가 검수한다.
- Accounts: 회원가입 없음. 시연 계정 4개(전임자, 후임자, 승인자, 관리자)를 `application-*.yml`의 `seed.members`로 서버 기동 시 생성한다(role `USER`/`ADMIN`, position `PREDECESSOR`/`SUCCESSOR`/`REVIEWER`, 상세는 `50-server-auth-security.md`). 나머지 데이터는 앱 기능(UI/API)으로 입력하고, Gap은 Gap Detection 구현 전까지 API로 등록한다. 시연 직전 DB 복구는 사용자가 한다.

## Still open

- 아래는 아직 정해지지 않았다. 확정 전에는 사용자 지시 없이 구현하지 않는다.
- UI 방향과 mock 디자인(합의 후 구현, `70-web.md`)
- SUH AI 연동 정보와 전체 API 엔드포인트 명세(이슈 #4, LLM은 `60-server-llm.md`)
- 합성 데이터와 시연 대본의 세부 내용
- 서비스 설정 동적 관리와 `ADMIN` 계정 기능(나중)

## Reference documents

- 사용자 개인 Obsidian vault의 `Projects/WHO-IM`, `Knowledge/WHO-IM Master Context v3.1 정리`(v1.0보다 우선), `Knowledge/WHO-IM 1차 기획안 제출본 기록`(작업본보다 우선)이 기준이다. 팀원은 볼 수 없을 수 있으므로 이 문서의 요약만으로 이해되어야 하고, vault 접근을 전제하지 않는다.
