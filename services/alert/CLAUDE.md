# CLAUDE.md — @Backend-Engineer-Agent (Alert Service)

> **⚠️ 작업 시작 전 `/LAWS.md`를 반드시 읽을 것. LAWS.md는 이 파일보다 우선한다.**

## 역할
Kafka에서 텔레메트리 이벤트를 소비하여 이상 징후를 탐지하고 Alert 이벤트를 발행하는 Alert Service를 구현합니다.
완료 후 `contracts/kafka-schemas/alert-event.yaml`을 작성합니다.

---

## 참조 문서 (작업 전 반드시 읽을 것)
- `/PRODUCT_SENSE.md` — 핵심 기능 중 "Real-time Alerting" 섹션
- `/DESIGN.md` — Stateless, Decoupling(Kafka 비동기), Idempotency 원칙
- `/RELIABILITY.md` — Circuit Breaker Pattern, Dead Letter Topic 요구사항
- `/QUALITY_SCORE.md` — 단위 테스트 커버리지 80% 이상

---

## 인프라 연동 정보 읽기
작업 시작 전 `contracts/infra-outputs.yaml`을 읽고 아래 값을 확인합니다:
- `msk.bootstrap_servers` — Kafka 연결 주소
- `msk.topics.telemetry_raw` — 소비할 토픽명
- `msk.topics.alert_events` — 발행할 토픽명
- `msk.topics.dead_letter` — DLT 토픽명

---

## 구현 범위

### Kafka Consumer
- 토픽: `telemetry-raw` 구독
- Consumer Group ID: `alert-service-group`

### 이상 징후 탐지 로직
| 조건 | Alert 타입 | Severity |
|---|---|---|
| temperature > 60°C | TEMPERATURE_EXCEEDED | CRITICAL |
| temperature > 50°C | TEMPERATURE_EXCEEDED | WARNING |
| voltage < 10V 또는 voltage > 60V | VOLTAGE_CRITICAL | CRITICAL |
| GPS 데이터 5회 연속 누락 | GPS_LOST | WARNING |

### 장애 내성
- **Circuit Breaker:** 하위 서비스 장애 시 전파 차단 (Resilience4j 또는 동등 라이브러리)
- **Dead Letter Topic:** 처리 3회 실패 메시지는 `dead-letter-topic`으로 전송

### Kafka Producer
- 탐지된 이상 징후를 `alert-events` 토픽에 발행
- `contracts/kafka-schemas/alert-event.yaml` 스키마 준수

---

## 품질 게이트
- 단위 테스트 커버리지 80% 이상
- 각 Alert 조건별 테스트 케이스 필수
- Circuit Breaker 동작 테스트 케이스 필수

---

## contracts/ 출력 규칙
완료 시 아래 파일을 작성합니다:
- `contracts/kafka-schemas/alert-event.yaml` — 실제 발행하는 Alert 이벤트 스키마 (status: completed 로 업데이트)

---

## 금지 사항
- `infra/`, `cicd/`, 다른 `services/` 디렉토리 수정 금지
- `contracts/infra-outputs.yaml` 수정 금지
- `contracts/kafka-schemas/telemetry-event.yaml` 수정 금지 (읽기만 허용)
