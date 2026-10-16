# Delivery

전역 git 규칙(force 계열 금지, `.gitignore` 존중, 커밋 형식)은 `~/.claude/rules/common/git-workflow.md`를 따르고 여기서는 이 프로젝트의 차이만 적는다. 서버 yml 키 정의는 `50-server-auth-security.md`, 테스트 정책은 `80-testing.md`를 본다.

## Git and release

- Authority: commit, push, PR, 브랜치 생성은 사용자가 지시할 때만 한다(`/prp-commit`, `/prp-pr` 등 스킬 지시 포함).
- Work location: `main`에서 직접 할지 이슈 브랜치에서 할지는 사용자가 정한다. 지정이 없으면 묻는다.
- Issue: 이슈를 만들면 ISSUE-HELPER가 브랜치 `YYYYMMDD_#N_제목`을 자동 생성한다(`ISSUE_HELPER_CREATE_BRANCH: "true"`, base `main`). 브랜치명에 `#`가 있으니 셸에서 따옴표로 감싼다.
- Commit: `<type>: <한국어 설명>` 한 줄, scope 없음. type은 feat, fix, refactor, docs, test, chore, perf, ci. 릴리즈 분류기는 style, build도 해석한다(로그에 보여도 고치지 않는다).
- Staging: `git add <경로>`로 경로를 지정한다. `git add -A`, `git add .`는 쓰지 않는다.
- No `[skip ci]`: 커밋 메시지에 넣지 않는다. 릴리즈 게이트를 우회한다(봇만 쓴다).
- Mode: `version.yml`의 `metadata.template.branches.mode`는 `trunk-based`다. `main` push마다 PROJECT-COMMON-RELEASE-PUBLISH가 버전을 올린다. feat는 minor, `!` 표기 또는 `BREAKING CHANGE:` 푸터는 major, 그 외는 patch다.
- main push = deploy + release: 태그 `vX.Y.Z`, GitHub Release, CHANGELOG 갱신. `server/**` 또는 `web/**`가 바뀌면 해당 CD도 실행된다. 문서만 바꿔도 릴리즈된다. 그래서 사용자가 명시적으로 지시할 때만 `main`에 push한다.
- Release bot: 릴리즈 후 봇이 `chore(version): release vX.Y.Z docs [skip ci]`를 `main`에 push하고 `version.yml`, `server/build.gradle`·`web/package.json`의 `version`, `CHANGELOG.json`, `CHANGELOG.md`를 바꾼다.
- Sync: `main`에 push한 뒤 추가 커밋 전에는 `git fetch` 후 봇 커밋을 일반 병합(merge 또는 fast-forward)으로 가져온다.
- CI-owned: `version.yml`, `server/build.gradle`, `web/package.json`의 `version`은 직접 수정하지 않는다.
- README: 루트 README가 없다. README 버전 줄 갱신은 루트 README가 생기기 전까지 효과가 없다.
- Forbidden: 사용자 지시 없는 `push`·브랜치 생성, `git tag v*` 생성·push, `gh release create`, `gh workflow run`, 로컬에서 서버로 docker/ssh 직접 배포, `.gitignore` 제외 파일 강제 추가.

## Secrets and env

| 파일 | Git | 내용 |
| --- | --- | --- |
| `server/src/main/resources/application.yml` | 추적 | 공통 설정. 민감 정보 금지. 기본 프로필 `prod` |
| `server/src/main/resources/application-dev.yml` | 무시 | 로컬 실행용. 존재 여부와 내용은 사용자가 관리한다 |
| `server/src/main/resources/application-prod.yml` | 무시 | 배포용. CI/CD가 Secret `APPLICATION_PROD_YML`로 덮어쓴다 |
| `web/.env.local` | 무시 | 로컬 환경값 |
| `web/.env.production` | 무시 | 배포 빌드용. CI가 Secret `ENV_FILE`로 주입한다 |

- Ignore: 루트 `.gitignore`가 `.env.*`, `application-*.yml`, `.idea`, `docs`, `.docs`, `issue`, `report`, `.issue`, `.report`, `web/web.iml`을 무시한다. 패키지·디렉터리 이름에 `docs`, `issue`, `report`를 쓰지 않는다.
- Read: `application-*.yml`과 `web/.env.*`는 읽을 수 있으나 키 이름만 확인하고 값은 출력하지 않는다(예: `grep -oE '^[A-Z_]+' web/.env.local`). 값이 필요하면 사용자에게 묻는다.
- Edit: 수정은 사용자 승인 후에만 한다. 서버 설정값을 추가할 때는 키 이름과 위치를 알려 주고 값은 사용자가 채운다.
- Keys: 서버 yml 키는 `seed.members`(username, password, name, role, position 목록)와 `jwt.secret-key`, `jwt.access-exp-millis`, `jwt.issuer`(이슈 #2에서 도입)이고, LLM 키는 `llm.suh-ai.base-url`, `llm.suh-ai.api-key`, `llm.suh-ai.model`(이슈 #4, `60-server-llm.md`)이다. 값은 문서·코드에 쓰지 않는다.
- Examples: `.env.example` 등 example/템플릿 파일은 만들지 않는다.
- `.idea/`: DB 데이터소스 메타데이터가 있으므로 읽거나 수정하지 않는다.
- Exposure: 값을 커밋, 로그, PR, 대화에 노출하지 않는다. 노출됐다면 즉시 알리고 폐기(rotate)를 안내한다.
- Web: `VITE_*` 변수는 번들에 포함되어 공개된다. 비밀값을 넣지 않는다.
- Flyway: 마이그레이션 SQL은 에이전트가 작성한다(KST 타임스탬프 파일명, 규칙은 `30-server-persistence.md`). 마이그레이션은 `server/**` 변경이므로 `main` push 시 서버 CD가 실행된다.
- DB: 초기화·drop·truncate·볼륨 삭제는 하지 않는다. 필요하면 원인과 절차만 안내한다.

| Secret | 용도 |
| --- | --- |
| `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` | Docker Hub 로그인·푸시 |
| `SERVER_HOST`, `SERVER_USER`, `SERVER_PASSWORD` | 배포 서버 SSH(password 인증) |
| `SSH_KEY` | 선언만 되어 있고 password 인증이라 미사용 |
| `APPLICATION_PROD_YML` | `application-prod.yml` 전체 내용 |
| `ENV_FILE` | `web/.env.production` 전체 내용. 없으면 `ENV`를 대신 읽는다 |
| `WORKFLOW_PAT` | 선택. 릴리즈 후 워크플로우 연쇄 실행 |
| `AI_API_KEY` | 선택. 릴리즈·PR 요약용 (Variables: `AI_API_BASE_URL`, `AI_MODEL`) |

- Caveat(LLM 키): `main`에 push하기 전에 GitHub Secret `APPLICATION_PROD_YML`에 `llm.suh-ai.*` 세 키가 있어야 한다. 하나라도 비면 `SuhAiProperties` 검증으로 앱 기동이 실패하고 CD 헬스체크(`/actuator/health`)에서 뒤늦게 실패한다.
- Firebase: `firebase-adminsdk.json` 주입 단계는 서버 워크플로우에 주석 처리되어 있다. 필요할 때만 해제한다.

## CI and CD

`project-auto-wizard`로 생성한 GitHub Actions를 쓴다(`version.yml` template 0.17.0). 기준은 `.github/workflows/`의 실제 파일이며 아래는 요약이다.

| 파일 | 트리거 | 내용 |
| --- | --- | --- |
| `PROJECT-SPRING-CI.yml` | push/PR `main`, `server/**` | JDK 21, prod yml 주입 후 `./gradlew clean`, `compileJava compileTestJava`, `build -x test`. 테스트·Docker 없음 |
| `PROJECT-REACT-CI.yaml` | push/PR `main`, `web/**` | Node 22, `.env.production` 주입, `npm ci`, `npm test`(test 스크립트가 있을 때만), `npm run build` |
| `PROJECT-SPRING-SIMPLE-CICD.yaml` | push `main` + `server/**` | 이미지 `<DOCKERHUB_USERNAME>/whoim-server:main`, 컨테이너 `whoim-server`, `8087`→`8080`, 볼륨 `/volume1/project/whoim/server`→`/mnt/whoim-server`, `--restart unless-stopped`, SSH 2022 password 인증, `/actuator/health` 120회×1초 확인, `docker image prune -f` |
| `PROJECT-REACT-CICD.yaml` | push `main` + `web/**` | 이미지 `<DOCKERHUB_USERNAME>/whoim-web:main`, 컨테이너 `whoim-web`, `3006`→`80`(nginx) |
| `PROJECT-COMMON-RELEASE-PUBLISH.yaml` | push `main`, dispatch | 버전, 태그, Release, CHANGELOG |
| `PROJECT-COMMON-README-VERSION-UPDATE.yaml` | push `main`(trunk-based에서는 자체 스킵), dispatch | RELEASE-PUBLISH가 호출해 README 버전 줄 갱신 |
| `PROJECT-COMMON-ISSUE-HELPER.yaml` | issues | 브랜치명·커밋 제안, 브랜치 자동 생성 켜짐 |
| `PROJECT-COMMON-AI-PR-SUMMARY.yaml` | PR `main` | PR 요약 |

- Dockerfile: `server/Dockerfile`(Temurin 21 JRE alpine, `build/libs/app.jar` 복사, HEALTHCHECK), `web/Dockerfile`(nginx:alpine, CI가 빌드한 `dist` 복사, `nginx.conf`의 SPA 폴백).
- Build: `server/build.gradle`의 `bootJar` 이름 `app.jar` 고정과 plain `jar` 비활성은 Dockerfile이 `build/libs/app.jar`를 복사하므로 바꾸지 않는다. CD는 컨테이너를 `TZ=Asia/Seoul`로 실행하지만 코드는 시간대를 `Clock` 빈(`20-server-coding-style.md`)에만 의존한다.
- Pre-check: SIMPLE-CICD 사전 점검은 `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `SERVER_HOST`, `SERVER_USER`, `SERVER_PASSWORD`와 Dockerfile 존재만 검사한다. 비어 있으면 점검에서 실패하는 것이 정상이다.
- Caveat: `APPLICATION_PROD_YML`은 사전 점검 대상이 아니다. 비면 빈 prod yml로 이미지가 빌드되고 배포 후 헬스체크에서 뒤늦게 실패한다. REACT CD도 `ENV_FILE`(또는 `ENV`)이 비면 빈 `.env.production`으로 빌드되므로 값 존재를 확인한다.
- PR-PREVIEW: 워크플로우는 제거되었으나 `.github/.wizard/baseline.json`에 `PROJECT-SPRING-PR-PREVIEW.yaml`이 남아 있어 wizard 재실행 시 복구를 권할 수 있다. 복구하지 않는다.
- wizard: `project-auto-wizard` 재실행은 사용자가 직접 한다. Claude가 임의로 실행하지 않는다.
- Protected: `.github/scripts/*`, `.github/.wizard/baseline.json`, `version.yml`은 수정하지 않는다.
- Mismatch: `version.yml`의 `deploy.react.PROJECT_NAME`은 `whoim-server`로 남아 있으나 실제 워크플로우 값은 `whoim-web`이다. wizard 재실행 시 `deploy:` 블록이 재생성된다. 바꾸려면 사용자에게 먼저 확인한다.
- Edit scope: 워크플로우는 사용자가 요청한 범위만 고친다. 바꿀 때는 path 필터, env 이름, 시크릿 이름을 함께 확인한다.
- Caveat: 로깅 의존성(`com.chuseok22:logging`)이 개인 Nexus(`nexus.chuseok22.com`)에서 해석되므로 CI/CD 빌드가 Nexus 가용성에 종속된다. 장애 시 롤백은 `server/build.gradle`의 로깅 의존성·저장소 제거와 `@LogMonitoring` 삭제다.
- CI ≠ tests: CI는 서버 테스트를 실행하지 않는다. 통과가 테스트 통과를 뜻하지 않는다(`80-testing.md`).
