# Deployment

GitHub Actions workflow: `.github/workflows/ci-cd.yml`

## CI

다음 이벤트에서 자동 실행된다.

- `main`, `develop` 브랜치 push
- `main`, `develop` 대상 pull request
- 수동 실행: GitHub Actions `workflow_dispatch`

CI 단계:

1. Java 21 설정
2. Gradle 캐시 설정
3. `./gradlew test` (H2 인메모리 DB로 실행, 별도 DB 불필요)
4. (`main` 브랜치 push에 한해) `Dockerfile`로 이미지를 빌드해 GHCR(`ghcr.io/<owner>/<repo>`)에 `latest`, `<commit-sha>` 태그로 push

## Optional CD

`main` 브랜치 push에서만 실행된다. 아래 GitHub repository secrets가 모두 설정되어 있으면
배포 서버에 `docker-compose.prod.yml` + `.env`를 전송하고 `docker compose pull && up -d`로 컨테이너를 갱신한다.

배포 서버 접속:

- `DEPLOY_HOST`: 배포 서버 주소
- `DEPLOY_USER`: SSH 사용자
- `DEPLOY_SSH_KEY`: SSH private key
- `DEPLOY_PATH`: 서버에 compose 파일을 둘 경로 (예: `/opt/capstone`)

애플리케이션/DB 설정값 (`.env`로 전달됨, `.env.example` 참고):

- `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`, `ADMIN_PASSWORD`

GHCR 이미지 인증 (선택):

- `GHCR_TOKEN`: GHCR 패키지가 private일 때 배포 서버에서 `docker login ghcr.io`에 사용할 PAT(`read:packages`).
  패키지를 GitHub Packages에서 public으로 설정하면 생략 가능.

서버 사전 준비 사항:

- Docker, Docker Compose v2 (`docker compose`) 설치
- `DEPLOY_PATH` 디렉터리 쓰기 권한 (CI가 `docker-compose.yml`, `.env`를 이 경로에 생성)

배포 후 컨테이너 구성은 `docker-compose.prod.yml` 참고: `mysql` + `app`(GHCR 이미지) 2개 서비스로 구성되며,
`app`은 `mysql`의 healthcheck 통과 후 기동된다.
