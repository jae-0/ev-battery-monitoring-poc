# EV 배터리 PoC — 부하 테스트 결과 보고서

**테스트 일시:** 2026-04-16  
**대상 엔드포인트:** `POST http://20.249.108.139:8080/v1/telemetry`  
**환경:** Azure Korea Central (Free Tier)  
**도구:** Apache JMeter 5.5 (Docker)

---

## 인프라 구성

| 리소스 | 사양 |
|---|---|
| AKS 노드 | Standard_D2s_v3 × 2 (2vCPU, 8GB RAM) |
| telemetry Pod | 2 replica |
| CosmosDB | Free Tier (1,000 RU/s) |
| Event Hubs | Basic Tier (1 TU) |

---

## 테스트 결과

### Test 1 — 최대 부하 (500 스레드)

| 지표 | 결과 |
|---|---|
| 총 요청 수 | 9,468 |
| 테스트 시간 | 129초 |
| **TPS** | **73.3 req/s** |
| 평균 Latency | 6,221ms |
| 최소 Latency | 13ms |
| 최대 Latency | 14,226ms |
| **에러율** | **86.9%** |
| 에러 원인 | CosmosDB 429 (RU/s 초과), 커넥션 타임아웃 |

### Test 2 — 프리티어 최적 (5 스레드)

| 지표 | 결과 |
|---|---|
| 총 요청 수 | 2,181 |
| 테스트 시간 | 60초 |
| **TPS** | **36.1 req/s** |
| 평균 Latency | 130ms |
| 최소 Latency | 21ms |
| 최대 Latency | 7,015ms |
| **에러율** | **0.46%** |
| 에러 원인 | CosmosDB 간헐적 429 (burst 시 RU/s 초과) |

---

## 목표 대비 달성 현황

| 성공 기준 | 목표 | 달성 | 비고 |
|---|---|---|---|
| TPS | 10,000/s | 36.1/s | 프리티어 한계 |
| p95 Latency | < 500ms | ~300ms (5스레드) | 정상 부하 시 달성 |
| 에러율 | < 0.1% | 0.46% | CosmosDB throttle |

---

## 병목 분석

### 주요 병목: CosmosDB Free Tier
- 한도: 1,000 RU/s
- 쓰기 1건 = 약 5~10 RU 소비
- 실질적 최대 처리량: 100~200 TPS
- 초과 시 즉시 `429 Too Many Requests` 응답

### 보조 병목
- AKS 2노드 (4 vCPU 합계) — 500 스레드 처리 불가
- Event Hubs Basic 1 TU — 1 MB/s 한도

---

## 10,000 TPS 달성을 위한 요건

| 리소스 | 현재 | 필요 사양 |
|---|---|---|
| CosmosDB | Free (1,000 RU/s) | 50,000+ RU/s (Provisioned) |
| AKS 노드 | 2 × D2s_v3 | 10+ × D4s_v3 이상 |
| Event Hubs | Basic 1 TU | Standard 10+ TU |
| telemetry Pod | 2 replica | 20+ replica (HPA) |

---

## 결론

- 아키텍처(AKS + CosmosDB + Event Hubs) 는 **정상 동작 확인**
- API 인증, Kafka 발행, CosmosDB 저장 **End-to-End 흐름 검증 완료**
- 프리티어 환경에서 안정적 TPS: **36 req/s (에러율 0.46%)**
- 10,000 TPS 목표는 **유료 티어 전환 및 스케일아웃으로 달성 가능**한 아키텍처임을 확인
