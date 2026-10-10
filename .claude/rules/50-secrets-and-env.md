# Secrets And Env

## Files

| 파일 | Git | 내용 |
| --- | --- | --- |
| `server/src/main/resources/application.yml` | 추적 | 공통 설정. 민감 정보 금지 |
| `server/src/main/resources/application-dev.yml` | 무시 | 로컬 실행용. 현재 비어 있다(0 bytes). 사용자가 채운다 |
| `server/src/main/resources/application-prod.yml` | 무시 | 배포용. 로컬은 비어 있고 CI/CD가 Secret `APPLICATION_PROD_YML`로 생성한다 |
| `web/.env.local` | 무시 | 로컬 환경값 (루트 `.env.*`와 `web/.gitignore`의 `*.local`) |
| `web/.env.production` | 무시 | 배포 빌드용. CI가 Secret `ENV_FILE`로 주입한다 |

- Ignore rules: 루트 `.gitignore`가 `.env.*`, `application-*.yml`, `.idea`, `docs`, `.docs`, `issue`, `report`, `.issue`, `.report`, `web/web.iml`을 무시한다.
- Example files: `.env.example` 등 example/템플릿 파일은 만들지 않는다.
- `.idea/`: DB 데이터소스 메타데이터가 들어 있으므로 읽거나 수정하지 않는다.

## Handling rules

- Read: 읽을 수 있으나 값은 출력하지 않는다. 키 이름만 확인한다(예: `grep -oE '^[A-Z_]+' web/.env.local`, yml은 키 목록만). 값이 필요하면 사용자에게 묻는다.
- Edit: 수정은 사용자 승인 후에만 한다.
- Exposure: 값을 커밋, 로그, PR, 대화에 노출하지 않는다. 노출됐다면 즉시 알리고 폐기(rotate)를 안내한다.
- Web: `VITE_*` 변수는 번들에 포함되어 공개된다. 비밀값을 넣지 않는다. 현재 `VITE_API_BASE_URL`을 쓴다.
- Config values: 서버 설정값 추가 시 키 이름과 위치를 알려주고 값은 사용자가 채운다.

## GitHub Secrets (이름만)

| Secret | 용도 |
| --- | --- |
| `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` | Docker Hub 로그인·푸시 |
| `SERVER_HOST`, `SERVER_USER`, `SERVER_PASSWORD` | 배포 서버 SSH(password 인증) |
| `SSH_KEY` | 선언만 되어 있고 password 인증이라 미사용 |
| `APPLICATION_PROD_YML` | `application-prod.yml` 전체 내용 |
| `ENV_FILE` | `web/.env.production` 전체 내용. 없으면 `ENV`를 대신 읽는다 |
| `WORKFLOW_PAT` | 선택. 릴리즈 후 워크플로우 연쇄 실행 |
| `AI_API_KEY` | 선택. 릴리즈·PR 요약용 (Variables: `AI_API_BASE_URL`, `AI_MODEL`) |

- Firebase: `firebase-adminsdk.json` 주입 단계는 서버 워크플로우에 주석 처리되어 있다. 필요할 때만 주석을 해제한다.
- 상세: 시크릿이 비었을 때의 CD 동작은 `60-ci-cd.md`를 본다.
