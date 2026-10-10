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
- 본선 구현은 Hero Flow(Evidence-first, TRACE, Verified TRACE, Decision Reuse) 하나만 완성하는 방향이다. 상세는 미확정이다.

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

## Not decided yet

- 아래 영역은 추후 논의 대상이다. 확정 전에는 사용자 지시 없이 구현하지 않는다.
- 구현 범위(MVP, Hero Flow 상세)
- AI/LLM 규칙(LLMProvider 추상화, DLP, 사용할 LLM, API 키)
- 데이터·개인정보 규칙
- 인증·권한 구조(현재는 Spring Security 기본값. `SecurityConfig`에서 `/actuator/health`만 permitAll)

## Reference documents

- 사용자 개인 Obsidian vault의 `Projects/WHO-IM`, `Knowledge/WHO-IM Master Context v3.1 정리`(v1.0보다 우선), `Knowledge/WHO-IM 1차 기획안 제출본 기록`(작업본보다 우선)이 기준이다. 팀원은 볼 수 없을 수 있으므로 이 문서의 요약만으로 이해되어야 하고, vault 접근을 전제하지 않는다.
