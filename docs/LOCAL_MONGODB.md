# Local MongoDB with Docker

MongoDB를 로컬에 직접 설치하지 않고 Docker Compose로 실행한다.

## Start

```bash
docker compose up -d mongodb
```

상태 확인:

```bash
docker compose ps
docker compose logs -f mongodb
```

## Run Spring Boot with MongoDB

```bash
MONGODB_URI=mongodb://localhost:27017/capstone ./gradlew bootRun --args='--spring.profiles.active=mongo'
```

다른 포트를 써야 하면:

```bash
MONGODB_URI=mongodb://localhost:27017/capstone ./gradlew bootRun --args='--spring.profiles.active=mongo --server.port=8082'
```

## Check data

Mongo shell 접속:

```bash
docker compose exec mongodb mongosh capstone
```

컬렉션 확인:

```javascript
show collections
db.monitoring_events.find().sort({occurredAt: -1}).limit(5)
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
