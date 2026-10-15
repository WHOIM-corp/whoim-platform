---
paths:
  - "web/**"
---

# Web

## Stack

- Installed: React 19.2, Vite 8, TypeScript ~6.0, ESLint(typescript-eslint, react-hooks, react-refresh), npm. CI는 Node 22.
- Decided, install later: react-router, TanStack Query(서버 상태), Zustand(클라이언트 상태), CSS Modules, Prettier, vitest. UI 라이브러리는 필요 시 도입 전 사용자 확인.
- Not used: Tailwind를 쓰지 않는다. 확정된 패키지도 필요한 시점에 설치하고 미리 추가하지 않는다.

## Structure

- Policy override: `src/features/<name>/{components,hooks,api,...}` feature 단위로 구성하고 공용 코드는 `src/shared`에 둔다(전역 web/coding-style의 `src/components` 타입별 구성을 따르지 않는다).
- Current: `src/`에는 Vite 템플릿(`App.tsx`, `main.tsx`, `index.css`, `App.css`, `assets/`)만 있다.

## Screens and accounts

- Position-based: 메뉴와 화면은 로그인한 계정의 `position`(전임자 `PREDECESSOR`, 승인자 `REVIEWER`, 후임자 `SUCCESSOR`)으로 나눈다. 권한 모델은 `50-server-auth-security.md`를 따른다.
- Naming: Card 검수(Approve, Request Revision, Reject, Publish)와 Package 진행률 화면은 승인자 화면이다. 관리자(`role=ADMIN`)는 아직 기능이 없어 전용 화면을 만들지 않는다.
- Demo accounts: 시연용 4계정(전임자, 후임자, 승인자, 관리자) 빠른 전환 UI는 웹 이슈에서 구현한다. 계정 값은 코드·문서에 쓰지 않는다.
- Errors: 서버 에러 응답은 `{ errorCode, errorMessage }` 두 필드다. 화면에는 `errorMessage`를 쓰고 분기는 `errorCode`로 한다(`40-server-api-and-error.md`).
- API docs: 서버 Swagger는 `/docs/swagger`에서 확인한다.

## TypeScript and style

- TS 6에서 `strict`는 기본 적용이며 tsconfig에 키가 없어도 켜져 있다. `strict: false`로 끄거나 `any`로 우회하지 않는다.
- Imports: `tsconfig.app.json`에 `allowImportingTsExtensions`가 켜져 있어 `import App from './App.tsx'`처럼 확장자를 허용한다(기존 템플릿 스타일).
- tsconfig: `noUnusedLocals`, `noUnusedParameters`, `erasableSyntaxOnly`, `verbatimModuleSyntax`가 켜져 있다(`enum`, 파라미터 프로퍼티 사용 불가, 타입은 `import type`).
- Prettier(도입 시): 작은따옴표, 세미콜론 없음, `printWidth` 100, `trailingComma: all`. 기존 코드는 이미 세미콜론 없는 작은따옴표 스타일이다.

## Environment

- 파일: `web/.env.local`(로컬), `web/.env.production`(배포, CI가 `ENV_FILE` 시크릿으로 주입). 둘 다 gitignore.
- `VITE_*` 변수는 번들에 노출되므로 비밀값을 넣지 않는다. 현재 키는 `VITE_API_BASE_URL`이다.
- 상세와 열람 규칙은 `90-delivery.md`를 따른다.

## UI process

- Mockup first: Figma가 없다. UI 작업 전에 HTML 정적 mock 디자인을 먼저 보여주고 사용자와 합의한 뒤에만 `web/src`에 구현한다. 합의 전 구현 금지.
- Location: mock의 형식과 저장 위치는 사용자가 지정한다. 지정이 없으면 묻는다. 루트 `.gitignore`가 `docs/`, `.docs/`, `issue/`, `report/`, `.issue`, `.report`를 무시하므로 그 경로의 mock은 커밋되지 않는다.
- Design: 전역 "템플릿 같은 UI 금지"를 적용하고 UI 작업에는 `frontend-design` 스킬을 활용한다.
- Target: UI 언어는 한국어, 데스크톱(발표 화면) 우선.
- Policy override: 접근성 전용 작업(ARIA 보강, 키보드 탐색 구현, 접근성 테스트)은 하지 않는다. 시맨틱 HTML은 평소처럼 쓴다(전역 web/testing의 Accessibility 우선순위를 따르지 않는다).
- Verify: 구현 완료 후 사용자가 `npm run dev`로 직접 확인한다.

## Before reporting done

- `cd web && npm run lint && npm run build`를 실행하고 결과를 보고한다(`build`는 `tsc -b && vite build`).
- `package.json`의 `version`은 릴리즈 봇이 관리하므로 직접 수정하지 않는다.
