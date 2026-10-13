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
- 2단계 검수: 전임자 확정 → 회사 관리급 승인 → 후임자 공개. Capture와 Release는 다르다
- DLP: Fail-Closed 원칙

## Schedule (사용자 확인, 2026-10-10 기준)

- 1차 예선 결과 10/14, 본선 11/4(수) 18:00. 본선은 제출한 기획안을 메이킹한 결과물이다.
- 본선 구현 범위는 아래 "Implementation scope"를 따른다.

## Structure and stack

- Monorepo: `server/`, `web/`, `.github/`(워크플로우), 루트 `version.yml`
- server: Spring Boot 4.1.1, Java 21, Gradle(Groovy), JPA + PostgreSQL, Spring Security, Actuator. 루트 패키지 `com.chuseok22.whoim.server`. Thymeleaf starter와 `thymeleaf-extras-springsecurity6`도 의존성에 포함(용도 미정).
- web: React 19.2, Vite 8, TypeScript ~6.0, npm. CI는 Node 22.
- Deploy: Docker(Docker Hub) + SSH 서버(Synology 추정). API `https://api.whoim.chuseok22.com`, web `https://www.whoim.chuseok22.com` (도메인은 사용자 확인).

## Commands

- server run: `cd server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun` (application-dev.yml을 사용자가 채운 뒤)
- server build: `cd server && ./gradlew clean build -x test`
- web: `cd web && npm ci`, `npm run dev`, `npm run build`, `npm run lint`
- Caveat: 현재 `application-dev.yml`과 `application-prod.yml`은 비어 있고(0 bytes) `application.yml`에 datasource가 없다. 사용자가 dev yml을 채우고 `SPRING_PROFILES_ACTIVE=dev`로 실행하기 전에는 `bootRun`이 기동되지 않고 `ServerApplicationTests#contextLoads`도 실패한다.

## Implementation scope (사용자 확정, 2026-10-13 기준)

- Goal: Hero Flow(Evidence-first → TRACE → Verified TRACE → Decision Reuse) 하나만 완성한다. 본선(11/4)은 영상 + 현장 시연이고, 개발은 단독 약 3주다.
- P0: 데이터 모델과 상태 Enum, JWT 인증, 인터뷰(3~5턴, 텍스트만) → TRACE Card 생성, 전임자 리뷰(필드 수정 + Confirm), 관리자 핵심(Card 목록·상세, Approve, Request Revision, Package 진행률·READY·Publish), 후임자 Guided Decision Support(`PUBLISHED`+`APPROVED` Card만 → 역질문 → Card 제시, Settlement Batch 두 분기).
- P1: Evidence 업로드(`.md`·`.txt`)와 Gap Detection, 관리자 보조(Reject, 이력 타임라인, 사용자 관리), DLP(Rule/Regex).
- P2: Gap Detection 일반화, Fail-Closed 시연 코드, UI polish.
- Excluded: 실제 Connector, Scheduler, Enterprise TRACE Search, Versioning UI, 대시보드, 알림, 음성, SSO, 멀티테넌시, 시드 로더, 벡터 DB, Claim 단위 Evidence. 사용자 지시 없이 구현하지 않는다.
- Order: ① 모델·인증 골격 → ② 인터뷰·Card 생성·전임자 리뷰 → ③ 관리자 페이지 → ④ 후임자 Decision Support → ⑤ Evidence·Gap Detection → ⑥ DLP·Hard Rule 보강
- Quality: 관리자 핵심은 정상·예외·상태 오류까지 완성도를 높이고, 보조 기능은 단순하게 만든다.
- Data: 합성 데이터만 쓴다. 실제 회사·인물·내부 정보를 코드, 테스트, 스크린샷에 넣지 않는다. 합성 데이터와 시연 대본은 Claude가 초안을 쓰고 사용자가 검수한다.
- Initial data: 시드 로더는 없다. 초기 관리자 1명만 설정 값으로 생성하고 나머지는 앱 기능(UI/API)으로 입력한다. Gap은 Gap Detection 구현 전까지 API로 등록한다. 시연 직전 DB 복구는 사용자가 한다.

## Still open

- 아래는 아직 정해지지 않았다. 확정 전에는 사용자 지시 없이 구현하지 않는다.
- UI 방향과 mock 디자인(합의 후 구현, `20-web.md`)
- SUH AI 연동 정보와 전체 API 엔드포인트(한 이슈에서 정리)
- 합성 데이터와 시연 대본의 세부 내용

## Reference documents

- 사용자 개인 Obsidian vault의 `Projects/WHO-IM`, `Knowledge/WHO-IM Master Context v3.1 정리`(v1.0보다 우선), `Knowledge/WHO-IM 1차 기획안 제출본 기록`(작업본보다 우선)이 기준이다. 팀원은 볼 수 없을 수 있으므로 이 문서의 요약만으로 이해되어야 하고, vault 접근을 전제하지 않는다.
