# MVP API

## AI event ingest

`POST /api/ai/events`

```json
{
  "cameraId": "CAM-01",
  "bedId": "BED-01",
  "patientName": "김환자",
  "patientPosition": "right_edge",
  "posture": "exit_attempt",
  "guardrailUp": false,
  "caregiverPresent": false,
  "frameUrl": null,
  "roi": {"x": 18, "y": 18, "width": 62, "height": 62},
  "patientBox": {"x": 64, "y": 24, "width": 18, "height": 34}
}
```

위험도 계산 규칙:

- 침대 가장자리 근접: `+3`
- 가드레일 내려감: `+3`
- 앉음 또는 이탈 시도 자세: `+2`
- 보호 인력 부재: `+2`
- `0~2`: 정상, `3~5`: 주의, `6+`: 위험

## Dashboard data

- `GET /api/status/current`: 최신 상태
- `GET /api/status/summary`: 최근 100건 요약
- `GET /api/events?limit=20`: 이벤트 이력
- `GET /api/events?bedId=BED-01&riskLevel=DANGER&acknowledged=false&limit=20`: 필터링된 이벤트 이력
- `POST /api/events/{eventId}/ack`: 경고 확인 처리
- `GET /api/beds`: 침상별 최신 상태
- `GET /sse/status`: 실시간 상태 스트림
- `POST /api/questions`: 간단 상태 질의응답
- `POST /api/demo/events`: 시연용 상태 이벤트 1건 생성

## Run

```bash
./gradlew bootRun
```

MongoDB에 저장하려면 `mongo` 프로필을 사용한다.

```bash
MONGODB_URI=mongodb://localhost:27017/capstone ./gradlew bootRun --args='--spring.profiles.active=mongo'
```
