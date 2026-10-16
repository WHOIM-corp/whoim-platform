# Delivery And Review

## Git and release

- 권한: commit, push, PR, 브랜치 생성은 사용자가 지시할 때만 한다(스킬 지시 포함). 전역 git 규칙(force 계열 금지, `.gitignore` 존중)을 따른다.
- 작업 위치: `main`에서 할지 이슈 브랜치에서 할지는 사용자가 정한다. 지정이 없으면 묻는다. 이슈를 만들면 브랜치 `YYYYMMDD_#N_제목`이 자동으로 생성된다(`#`가 있어 셸에서 따옴표로 감싼다).
- 커밋: `<type>: <한국어 설명>` 한 줄, scope 없음. type은 feat, fix, refactor, docs, test, chore, perf, ci.
- 스테이징: `git add <경로>`. `git add -A`와 `git add .`는 쓰지 않는다.
- `main` push = 배포 + 릴리즈: 태그 `vX.Y.Z`, GitHub Release, CHANGELOG 갱신, 해당 `server/**`·`web/**` CD 실행. 문서만 바꿔도 릴리즈된다. 그래서 사용자가 명시적으로 지시할 때만 `main`에 push한다. `feat`는 minor, `!` 표기 또는 `BREAKING CHANGE:`는 major, 그 외는 patch다.
- 릴리즈 봇이 `chore(version): release vX.Y.Z docs [skip ci]`를 `main`에 push한다. `main`에 push한 뒤 추가 커밋 전에는 `git fetch`로 봇 커밋을 일반 병합으로 가져온다.
- 금지: `[skip ci]` 커밋 메시지, `v*` 태그 생성·push, `gh release create`, `gh workflow run`, 로컬에서 서버로의 docker·ssh 직접 배포.

## Report format

- report 단계에서 포함할 섹션: 변경 목적, 변경 파일, 위험 요소, 검증 결과, 남은 이슈.
- 저장 위치와 이름: `.report/` 디렉터리(없으면 생성), 파일명 `[YYYYMMDD]_[ISSUE#]_[간단한설명].md`(작업 완료일, 설명은 한글 또는 영문, 단어는 언더스코어로 구분). 예: `20261016_#1_도메인_모델과_상태_Enum.md`. `.report/`는 `.gitignore` 대상이라 커밋되지 않는다.
- 원칙: 작성자·작성일 필드를 넣지 않는다(파일명에 날짜가 있음). AI 도구명을 언급하지 않는다. 능동태와 키워드 중심 문장으로 쓴다. 민감 정보는 `{TOKEN}`, `{API_KEY}`, `{PASSWORD}`로 마스킹한다.
- 구조:

  ```markdown
  ### 📌 작업 개요
  [2-3줄 요약]

  ### 🎯 구현 목표 (기능 구현) 또는 🔍 문제 분석 (버그 수정)
  [목적 또는 문제 원인]

  ### ✅ 구현 내용
  #### [주요 변경사항 1]
  - **파일**: `경로/파일명`
  - **변경 내용**: [구체적인 설명]
  - **이유**: [왜 이렇게 수정했는지]

  ### 🔧 주요 변경사항 상세
  [코드 변경 내용과 특이사항]

  ### 🧪 검증 결과
  [lint, build, 테스트 결과. PASS/FAIL 명시]

  ### ⚠️ 위험 요소
  [없으면 "없음"]

  ### 📌 남은 이슈
  [후속 작업, 미검증 경로, TODO]
  ```

- 분석 프로세스: ① `git status`를 한 번만 실행해 변경 파일을 확인한다 ② 이슈 내용 기반으로 관련 파일만 고른다 ③ 고른 파일을 직접 읽어 분석한다 ④ 이후 추가 git 명령은 쓰지 않는다.

## Pull request expectations

- PR 제목: `[브랜치명] : [타입] : [설명] [이슈링크]`. 타입은 feat, fix, refactor, chore, docs. 예: `20261013_#2_JWT_인증과_역할_기반_접근_제한 : feat : JWT 로그인과 Member 기반 구현 https://github.com/WHOIM-corp/whoim-platform/issues/2`.
- PR 본문: `.report/` 보고서를 기반으로 변경 목적, 주요 변경 파일, 검증 결과를 적고 UI 변경이면 스크린샷을 넣는다.
- linked issue: 제목 또는 본문에 이슈 URL을 포함한다. reviewer 지정은 팀 규칙에 따른다.

## Delivery constraints

- 비밀과 환경 파일: `application-dev.yml`, `application-prod.yml`, `web/.env.*`는 gitignore다. 읽을 때는 키 이름만 확인하고 값은 출력하지 않는다(예: `grep -oE '^[A-Z_]+' web/.env.local`). 수정은 사용자 승인 후에만 한다. 값을 커밋, 로그, PR, 대화에 노출했다면 즉시 알리고 폐기(rotate)를 안내한다. `.env.example` 같은 example 파일은 만들지 않는다.
- prod 설정은 CI가 GitHub Secret `APPLICATION_PROD_YML`로 만들고, web은 `ENV_FILE`로 주입한다. `main`에 push하기 전에 두 Secret에 필요한 키가 있는지 확인한다(비면 배포 후 헬스체크에서 뒤늦게 실패한다). `VITE_*` 변수는 번들에 공개되므로 비밀값을 넣지 않는다.
- CI: `PROJECT-SPRING-CI.yml`(compile, `build -x test`), `PROJECT-REACT-CI.yaml`(`npm ci`, `npm run build`)가 `main` push와 PR에서 돈다. CD는 `main` push 시 `server/**`, `web/**` 변경에 따라 Docker 이미지를 빌드해 SSH 서버에 배포하고 `/actuator/health`를 확인한다.
- 빌드: `server/build.gradle`의 `bootJar` 이름 `app.jar` 고정과 plain `jar` 비활성은 Dockerfile이 `build/libs/app.jar`를 복사하므로 바꾸지 않는다.
- rollback: 배포 직후 기동·헬스체크 실패, 로그인 또는 인증 장애가 나면 이전 커밋으로 revert PR을 만들어 즉시 롤백한다.
- feature flag: 사용하지 않는다.
