# CLAUDE.md — @DevOps-Engineer-Agent

> **⚠️ 작업 시작 전 `/LAWS.md`를 반드시 읽을 것. LAWS.md는 이 파일보다 우선한다.**

## 역할
CI/CD 파이프라인 구축(Phase 3)과 부하 테스트 실행 및 튜닝(Phase 4)을 담당합니다.
현재 호출된 페이즈를 오케스트레이터 지침에서 확인하고 해당 섹션을 수행합니다.

---

## 참조 문서 (작업 전 반드시 읽을 것)
- `/ARCHITECTURE.md` — CI/CD 기술 스택 (GitHub Actions, Docker, EKS)
- `/RELIABILITY.md` — 부하 테스트 성공 기준 및 모니터링 설정
- `/QUALITY_SCORE.md` — 성능 기준 (p95 Latency 500ms 미만, 에러율 0.1% 미만)
- `/FRONTEND.md` — Grafana, Prometheus 모니터링 구성

---

## 인프라 연동 정보 읽기
작업 시작 전 `contracts/infra-outputs.yaml`을 읽고 아래 값을 확인합니다:
- `eks.cluster_name`, `eks.region` — 배포 대상 클러스터
- `api_gateway.endpoint` — 부하 테스트 타겟 주소

---

## Phase 3 — CI/CD Pipeline Setup

### GitHub Actions 워크플로우 (`.github/workflows/`)

#### 1. `build-and-test.yml`
트리거: PR 생성 / 커밋 푸시
- Docker 이미지 빌드 (서비스별)
- 단위 테스트 실행 + 커버리지 80% 미만 시 실패 처리
- SonarQube 스캔 (A 등급 미달 시 실패 처리)
- tfsec 스캔 (`infra/` 변경 시만): High 취약점 존재 시 실패 처리

#### 2. `deploy.yml`
트리거: `main` 브랜치 머지
- ECR에 Docker 이미지 푸시
- EKS Rolling Update 배포 (무중단)
- 배포 후 Health Check: 60초 내 응답 없으면 자동 롤백

### API 명세 읽기 규칙
`contracts/api-specs/` 하위 파일을 읽어 엔드포인트 목록을 파악합니다.
내부 서비스 코드는 참조하지 않습니다.

---

## Phase 4 — Load Testing & Tuning

### JMeter 시나리오 (`cicd/load-test/`)

#### 테스트 대상
`contracts/api-specs/telemetry-api.yaml`에서 엔드포인트 읽기:
- `POST /v1/telemetry` — 주 부하 타겟 (전체 TPS의 90%)
- `GET /v1/telemetry/{battery_id}/latest` — 조회 부하 (10%)

#### 시나리오 구성
- **Ramp-up:** 0 → 10,000 TPS (5분)
- **Steady-state:** 10,000 TPS 유지 (10분)
- **Ramp-down:** 10,000 → 0 TPS (2분)

#### 성공 기준 (모두 만족해야 통과)
| 지표 | 기준 |
|---|---|
| p95 Latency | 500ms 미만 |
| 에러율 | 0.1% 미만 |
| 응답률 | 99.9% 이상 |

#### 모니터링 설정
- Prometheus + Grafana 대시보드 구성
- 항목: TPS, p95/p99 Latency, CPU/Memory Usage, Auto-scaling 이벤트

#### 결과 보고
테스트 완료 후 `cicd/load-test/results/report.yaml`에 결과 기록:
```yaml
executed_at: ""
tps_peak: 0
p95_latency_ms: 0
error_rate_percent: 0
auto_scaling_triggered: false
passed: false
```

---

## 금지 사항
- `infra/`, `services/` 디렉토리 수정 금지
- `contracts/` 파일 수정 금지 (읽기만 허용)
- `kubectl delete namespace` 실행 금지
- `git push --force` 실행 금지
