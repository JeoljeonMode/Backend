# Local MySQL with Docker

MySQL을 로컬에 직접 설치하지 않고 Docker Compose로 실행한다.

## Start

```bash
docker compose up -d mysql
```

상태 확인:

```bash
docker compose ps
docker compose logs -f mysql
```

기본 접속 정보 (`.env.example` 참고, 필요 시 `.env`로 복사해 값 변경):

- DB: `capstone`
- User/Password: `capstone` / `capstone`
- Root Password: `root`

## Run Spring Boot with MySQL

```bash
./gradlew bootRun
```

기본값으로 `localhost:3306`의 `capstone` 데이터베이스에 접속한다. 접속 정보를 바꾸고 싶다면 환경변수로 덮어쓴다.

```bash
DB_HOST=localhost DB_PORT=3306 DB_NAME=capstone DB_USERNAME=capstone DB_PASSWORD=capstone ./gradlew bootRun
```

테이블은 `spring.jpa.hibernate.ddl-auto=update` 설정에 따라 애플리케이션 기동 시 자동 생성/갱신된다.

## Check data

MySQL 클라이언트 접속:

```bash
docker compose exec mysql mysql -ucapstone -pcapstone capstone
```

테이블 확인:

```sql
SHOW TABLES;
SELECT * FROM monitoring_events ORDER BY occurred_at DESC LIMIT 5;
```

## Stop

컨테이너만 중지:

```bash
docker compose down
```

데이터까지 삭제:

```bash
docker compose down -v
```
