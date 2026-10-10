# CI/CD

`project-auto-wizard`로 생성한 GitHub Actions를 쓴다(`version.yml` template 0.17.0). 기준은 `.github/workflows/`의 실제 파일이며 이 문서는 그 요약이다.

## Workflows

| 파일 | 트리거 | 내용 |
| --- | --- | --- |
| `PROJECT-SPRING-CI.yml` | push/PR `main`, `server/**` 필터 | JDK 21, prod yml 주입 후 `./gradlew clean`, `compileJava compileTestJava`, `build -x test`. 테스트·Docker 없음 |
| `PROJECT-REACT-CI.yaml` | push/PR `main`, `web/**` 필터 | Node 22, `.env.production` 주입, `npm ci`, `npm test`(test 스크립트가 있을 때만), `npm run build` |
| `PROJECT-SPRING-SIMPLE-CICD.yaml` | push `main` + `server/**` | 이미지 `<DOCKERHUB_USERNAME>/whoim-server:main`, 컨테이너 `whoim-server`, `8087`→`8080`, 볼륨 `/volume1/project/whoim/server`→`/mnt/whoim-server`, `--restart unless-stopped`, SSH 2022 password 인증, `/actuator/health` 120회×1초 확인, `docker image prune -f` |
| `PROJECT-REACT-CICD.yaml` | push `main` + `web/**` | 이미지 `<DOCKERHUB_USERNAME>/whoim-web:main`, 컨테이너 `whoim-web`, `3006`→`80`(nginx) |
| `PROJECT-COMMON-RELEASE-PUBLISH.yaml` | push `main`, dispatch | 버전, 태그, Release, CHANGELOG (`40-git-and-delivery.md`) |
| `PROJECT-COMMON-README-VERSION-UPDATE.yaml` | push `main`(trunk-based에서는 자체 스킵), dispatch | RELEASE-PUBLISH가 dispatch로 호출해 README 버전 줄 갱신(README 생기기 전에는 효과 없음) |
| `PROJECT-COMMON-ISSUE-HELPER.yaml` | issues | 브랜치명·커밋 제안, 브랜치 자동 생성 켜짐 |
| `PROJECT-COMMON-AI-PR-SUMMARY.yaml` | PR `main` | PR 요약 |

- PR-PREVIEW 워크플로우는 제거되었다. `.github/.wizard/baseline.json`에는 아직 `PROJECT-SPRING-PR-PREVIEW.yaml`이 남아 있어 wizard 재실행 시 복구를 권할 수 있다. 복구하지 않는다.
- Dockerfile: `server/Dockerfile`(Temurin 21 JRE alpine, `build/libs/app.jar` 복사, HEALTHCHECK), `web/Dockerfile`(nginx:alpine, CI가 빌드한 `dist` 복사, `nginx.conf`의 SPA 폴백).

## Secrets behavior

- Pre-check: SIMPLE-CICD의 사전 점검은 `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `SERVER_HOST`, `SERVER_USER`, `SERVER_PASSWORD`(password 모드)와 Dockerfile 존재만 검사한다. 비어 있으면 점검에서 실패하는데 정상 동작이다.
- Caveat: `APPLICATION_PROD_YML`은 사전 점검 대상이 아니다. 비어 있으면 빈 prod yml로 이미지가 빌드되고, 배포 후 헬스체크(120×1초)에서 뒤늦게 실패한다.
- REACT CD도 `ENV_FILE`(또는 `ENV`)이 비면 빈 `.env.production`으로 빌드될 수 있으므로 값 존재를 확인한다.

## Rules

- wizard: `project-auto-wizard` 재실행은 사용자가 직접 한다. Claude가 임의로 실행하지 않는다.
- Protected: `.github/scripts/*`, `.github/.wizard/baseline.json`, `version.yml`은 수정하지 않는다.
- Mismatch: `version.yml`의 `deploy.react.PROJECT_NAME`은 `whoim-server`로 남아 있으나 실제 워크플로우 값은 `whoim-web`이다. wizard 재실행 시 `deploy:` 블록이 재생성된다. 바꾸려면 사용자에게 먼저 확인한다.
- Edit scope: 워크플로우는 직접 수정할 수 있으나 사용자가 요청한 범위만 고친다. 바꿀 때는 path 필터, env 이름, 시크릿 이름을 함께 확인한다.
- CI ≠ tests: CI는 서버 테스트를 실행하지 않는다. 통과가 테스트 통과를 뜻하지 않는다(`30-testing.md`).
