# Git And Delivery

전역 git 규칙(force 계열 금지, `.gitignore` 존중, 커밋 형식)은 `~/.claude/rules/common/git-workflow.md`를 따르고 여기서는 이 프로젝트의 차이만 적는다.

## Authority

- Principle: commit, push, PR, 브랜치 생성은 사용자가 지시할 때만 한다(`/prp-commit`, `/prp-pr` 등 스킬 지시 포함).
- Work location: `main`에서 직접 할지 이슈 브랜치에서 할지는 사용자가 정한다. 지정이 없으면 묻는다. 간단한 작업은 `main`에서 진행할 수 있다.
- Issue: 이슈를 만들면 ISSUE-HELPER가 브랜치 `YYYYMMDD_#N_제목`을 자동 생성한다(`ISSUE_HELPER_CREATE_BRANCH: "true"`, base `main`).

## Commit

- Format: `<type>: <한국어 설명>`, scope 없이 한 줄.
- Types: feat, fix, refactor, docs, test, chore, perf, ci. (릴리즈 분류기는 style, build도 해석한다. 로그에서 보이더라도 고치지 않는다.)
- Staging: `git add <경로>`로 경로를 지정한다. `git add -A`, `git add .`는 쓰지 않는다.
- No `[skip ci]`: 커밋 메시지에 `[skip ci]`를 넣지 않는다. 릴리즈 게이트를 우회한다(봇만 쓴다).
- Issue template, PR 머지 방식: 사용자가 스킬로 지시하는 대로 한다.

## main push = deploy + release

- Mode: `version.yml`의 `metadata.template.branches.mode`는 `trunk-based`다. `main` push마다 PROJECT-COMMON-RELEASE-PUBLISH가 버전을 올린다. feat는 minor, `!` 표기 또는 `BREAKING CHANGE:` 푸터는 major, 그 외는 patch다.
- Outputs: 태그 `vX.Y.Z`, GitHub Release, CHANGELOG 갱신. `server/**` 또는 `web/**`가 바뀌면 해당 CD도 실행된다. 문서만 바꿔도 릴리즈된다.
- Rule: 그래서 사용자가 명시적으로 지시할 때만 `main`에 push한다.
- First release: 아직 태그가 없어(`git tag` 비어 있음) 첫 릴리즈는 전체 이력을 분류한다. 이력에 feat가 있으므로 첫 태그는 `v0.1.0`으로 예상되나 실제 동작은 검증하지 못했다.

## Release bot

- Bot commit: 릴리즈 후 봇이 `chore(version): release vX.Y.Z docs [skip ci]`를 `main`에 push하며 `version.yml`, `server/build.gradle`의 `version`, `web/package.json`의 `version`, `CHANGELOG.json`, `CHANGELOG.md`를 바꾼다(처음에는 CHANGELOG가 생성된다).
- Sync: `main`에 push한 뒤 추가 커밋 전에는 `git fetch` 후 봇 커밋을 일반 병합(merge 또는 fast-forward)으로 가져온다.
- CI-owned: `version.yml`, `server/build.gradle`, `web/package.json`의 `version`은 CI가 관리한다. 직접 수정하지 않는다.
- README: README가 아직 없다. README 갱신은 README가 생기기 전까지 릴리즈 효과로 기대하지 않는다.

## Forbidden without explicit instruction

- 사용자 지시 없는 `push`, 브랜치 생성
- `git tag v*` 생성·push, `gh release create`, `gh workflow run`(workflow_dispatch)
- 로컬에서 서버로 docker/ssh 직접 배포
- `git add -A`, `.gitignore`로 제외된 파일 강제 추가(전역 force 금지 참조)
