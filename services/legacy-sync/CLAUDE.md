# CLAUDE.md — @Backend-Engineer-Agent (Legacy Sync Service)

> **⚠️ 작업 시작 전 `/LAWS.md`를 반드시 읽을 것. LAWS.md는 이 파일보다 우선한다.**

## 역할
클라우드에서 처리된 배터리 상태 데이터를 기존  온프레미스 레거시 시스템에 주기적으로 동기화하는 Legacy Sync Service를 구현합니다.
완료 후 `contracts/api-specs/legacy-sync-api.yaml` 및 `contracts/kafka-schemas/legacy-sync-event.yaml`을 작성합니다.

---

## 참조 문서 (작업 전 반드시 읽을 것)
- `/PRODUCT_SENSE.md` — 핵심 기능 중 "Legacy Integration" 섹션
- `/DESIGN.md` — Stateless, Idempotency 원칙
- `/docs/exec-plans/tech-debt-tracker.md` — TD-03: 현재 1시간 벌크 인서트 방식 (PoC 범위)
- `/docs/generated/db-schema.md` — RDS PostgreSQL `batteries` 테이블 스키마
- `/docs/product-specs/new-device-onboarding.md` — 배터리 상태값 (IN_TRANSIT 등)
- `/QUALITY_SCORE.md` — 단위 테스트 커버리지 80% 이상

---

## 인프라 연동 정보 읽기
작업 시작 전 `contracts/infra-outputs.yaml`을 읽고 아래 값을 확인합니다:
- `msk.bootstrap_servers` — Kafka 연결 주소
- `msk.topics.legacy_sync_queue` — 소비할 토픽명
- `rds.endpoint`, `rds.port`, `rds.database` — PostgreSQL 연결 정보
- `rds.secret_arn` — DB 자격증명 (Secrets Manager)

---

## 구현 범위

### 동기화 방식 (PoC 범위: TD-03 기술 부채 허용)
- **스케줄:** 1시간 주기 벌크 인서트 (Cron Job)
- **대상 데이터:** RDS `batteries` 테이블의 status 변경 이력
- **전송 방식:** 레거시 시스템의 REST API 호출 (HTTP POST, 배치)

### API (내부 관리용)
| Method | Path | 설명 |
|---|---|---|
| POST | `/v1/legacy-sync/trigger` | 수동 동기화 즉시 트리거 (운영자용) |
| GET | `/v1/legacy-sync/status` | 마지막 동기화 시간 및 결과 조회 |

### 비즈니스 로직
1. RDS에서 마지막 동기화 시점 이후 변경된 `batteries` 레코드 조회
2. 레거시 시스템 API 포맷으로 변환
3. Idempotency 보장: 동일 `battery_id + updated_at` 재전송 시 무시
4. 전송 성공 시 동기화 타임스탬프 업데이트

---

## 품질 게이트
- 단위 테스트 커버리지 80% 이상
- Idempotency 검증 테스트 케이스 필수
- 레거시 API 실패 시 재시도 로직 테스트 필수

---

## contracts/ 출력 규칙
완료 시 아래 두 파일을 작성합니다:
- `contracts/api-specs/legacy-sync-api.yaml` — OpenAPI 3.0 명세 (내부 관리 API)
- `contracts/kafka-schemas/legacy-sync-event.yaml` — 레거시 동기화 이벤트 스키마 (status: completed 로 업데이트)

---

## 금지 사항
- `infra/`, `cicd/`, 다른 `services/` 디렉토리 수정 금지
- `contracts/infra-outputs.yaml` 수정 금지
- 레거시 온프레미스 시스템 직접 접속 금지 (PoC에서는 Mock 사용)
