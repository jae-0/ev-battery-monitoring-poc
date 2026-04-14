# CLAUDE.md — @Backend-Engineer-Agent (Telemetry Service)

> **⚠️ 작업 시작 전 `/LAWS.md`를 반드시 읽을 것. LAWS.md는 이 파일보다 우선한다.**

## 역할
IoT 단말기로부터 배터리 센서 데이터를 수신하고 Kafka에 발행하는 Telemetry Service를 구현합니다.
완료 후 `contracts/api-specs/telemetry-api.yaml` 및 `contracts/kafka-schemas/telemetry-event.yaml`을 작성합니다.

---

## 참조 문서 (작업 전 반드시 읽을 것)
- `/PRODUCT_SENSE.md` — 핵심 기능 중 "IoT Data Ingestion" 섹션
- `/DESIGN.md` — Stateless, Idempotency 원칙
- `/docs/generated/db-schema.md` — DynamoDB 스키마 (battery_id, timestamp, temperature, voltage, gps_lat, gps_lng)
- `/QUALITY_SCORE.md` — 단위 테스트 커버리지 80% 이상

---

## 인프라 연동 정보 읽기
작업 시작 전 `contracts/infra-outputs.yaml`을 읽고 아래 값을 확인합니다:
- `msk.bootstrap_servers` — Kafka 연결 주소
- `msk.topics.telemetry_raw` — 발행할 토픽명
- `dynamodb.table_name` — DynamoDB 테이블명
- `api_gateway.endpoint` — API Gateway 엔드포인트

---

## 구현 범위

### API (외부 클라이언트 → API Gateway → Telemetry Service)
| Method | Path | 설명 |
|---|---|---|
| POST | `/v1/telemetry` | IoT 단말기에서 배터리 센서 데이터 수신 |
| GET | `/v1/telemetry/{battery_id}/latest` | 특정 배터리 최신 상태 조회 |

### 비즈니스 로직
1. API Key 검증 (AWS Secrets Manager 참조)
2. 수신 데이터 유효성 검사 (온도, 전압, GPS 필드 필수)
3. Idempotency 보장: `battery_id + timestamp` 조합 중복 수신 시 무시
4. DynamoDB에 원본 데이터 저장
5. `telemetry-raw` Kafka 토픽에 이벤트 발행

### Kafka 메시지 형식
`contracts/kafka-schemas/telemetry-event.yaml` 스키마 준수

---

## 품질 게이트
- 단위 테스트 커버리지 80% 이상
- Idempotency 검증 테스트 케이스 필수 포함

---

## contracts/ 출력 규칙
완료 시 아래 두 파일을 작성합니다:
- `contracts/api-specs/telemetry-api.yaml` — OpenAPI 3.0 명세
- `contracts/kafka-schemas/telemetry-event.yaml` — 실제 발행하는 메시지 스키마 (status: completed 로 업데이트)

---

## 금지 사항
- `infra/`, `cicd/`, 다른 `services/` 디렉토리 수정 금지
- `contracts/infra-outputs.yaml` 수정 금지
