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
3. `./gradlew test`
4. `./gradlew bootJar`
5. `build/libs/*.jar` artifact 업로드

## Optional CD

`main` 브랜치 push에서만 실행된다. 아래 GitHub repository secrets가 모두 설정되어 있으면 jar를 서버에 업로드하고 systemd 서비스를 재시작한다.

- `DEPLOY_HOST`: 배포 서버 주소
- `DEPLOY_USER`: SSH 사용자
- `DEPLOY_SSH_KEY`: SSH private key
- `DEPLOY_PATH`: 서버 jar 배치 경로
- `DEPLOY_SERVICE_NAME`: systemd 서비스 이름. 생략 시 `capstone-backend`

서버에는 미리 systemd 서비스가 준비되어 있어야 한다. 예시:

```ini
[Unit]
Description=Capstone Backend
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/opt/capstone
ExecStart=/usr/bin/java -jar /opt/capstone/app.jar
Restart=always
Environment=SPRING_PROFILES_ACTIVE=mongo
Environment=MONGODB_URI=mongodb://localhost:27017/capstone

[Install]
WantedBy=multi-user.target
```
