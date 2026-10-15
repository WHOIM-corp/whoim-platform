# Changelog

**현재 버전:** 0.3.0  
**마지막 업데이트:** 2026-10-15T14:41:10Z  

---

## [0.3.0] - 2026-10-15

**✨ 기능**
- JWT_인증과_역할_기반_접근_제한 — JWT 로그인과 역할 기반 접근 제한 추가
- JWT_인증과_역할_기반_접근_제한 — Member 엔티티와 시드 계정 생성 추가

**📝 문서**
- 민감정보 로깅 마스킹 미사용 결정 반영
- 인증 구현에 맞춰 서버 규칙 문서 갱신

**✅ 테스트**
- JWT 인증과 시드 계정 테스트 추가

**🔧 변경사항**
- 로깅 민감정보 마스킹 설정 제거
- 로깅 민감정보 마스킹 설정 추가
- jjwt, Flyway, 로깅 라이브러리 의존성 추가

---

## [0.2.1] - 2026-10-15

**🔧 변경사항**
- 라벨 동기화 워크플로우 추가

---

## [0.2.0] - 2026-10-15

**✨ 기능**
- 공통_API_골격과_에러_시각_처리 — Swagger 문서 노출 설정 추가
- 공통_API_골격과_에러_시각_처리 — 전역 예외 처리 추가
- 공통_API_골격과_에러_시각_처리 — 서울 시간대 시각 처리 추가

**📝 문서**
- 서버 규칙 개정

**🔧 변경사항**
- GitHub 템플릿과 라벨 설정 추가
- 서버 의존성 추가(Lombok, validation, springdoc)

---

## [0.1.1] - 2026-10-15

**📝 문서**
- 확정된 구현 범위와 인증·LLM 규칙 반영

**🔧 변경사항**
- 서버 공통 설정(application.yml) 추가
- 도구 작업 폴더를 gitignore에 추가

---

## [0.1.0] - 2026-10-10

**✨ 기능**
- add security config exposing only health endpoint

**📝 문서**
- 에이전트 작업 규칙 추가

**🔧 변경사항**
- add wizard logs gitignore
- ignore web iml file
- add web ci and cd workflows
- add web dockerfile and nginx config
- initialize react web project
- add server ci and cd workflows
- add server dockerfile
- initialize spring boot server project with actuator
- add project-auto-wizard common workflows and scripts
- add root gitignore

---

