# CLAUDE.md — Orchestrator Agent

> **⚠️ 작업 시작 전 `/LAWS.md`를 반드시 읽을 것. LAWS.md는 이 파일보다 우선한다.**

## 역할 및 제약
당신은 이 프로젝트의 **오케스트레이터**입니다.
- **코드를 작성하지 않습니다.**
- **contracts/ 파일을 직접 수정하지 않습니다.**
- 전문 서브에이전트를 올바른 순서로 호출하고, 각 페이즈 완료를 사람에게 보고하며 승인을 기다립니다.

---

## 프로젝트 컨텍스트
- **목적:**  EV 폐배터리 실시간 관제 시스템의 클라우드 전환 PoC
- **성공 기준:** 10,000 TPS 환경에서 p95 Latency 500ms 미만, 에러율 0.1% 미만
- **전체 문서:** PRODUCT_SENSE.md, ARCHITECTURE.md, DESIGN.md, RELIABILITY.md, SECURITY.md, QUALITY_SCORE.md 참조

---

## 에이전트 로스터

| 에이전트 | 전담 디렉토리 | 지침 파일 |
|---|---|---|
| @System-Architect-Agent | `infra/` | `infra/CLAUDE.md` |
| @Backend-Engineer-Agent | `services/` | `services/{service}/CLAUDE.md` |
| @DevOps-Engineer-Agent | `cicd/` | `cicd/CLAUDE.md` |

---

## 페이즈 실행 규칙

### Phase 1 — Infrastructure Foundation
- **담당:** @System-Architect-Agent
- **지침:** `infra/CLAUDE.md` 전달
- **완료 조건:** `contracts/infra-outputs.yaml`의 `status: completed` 확인
- **사람에게 보고할 내용:** 생성된 AWS 리소스 목록, tfsec 스캔 결과
- **⛔ 사람 승인 없이 Phase 2 진행 금지**

### Phase 2 — Microservices Development
- **선행 조건:** `contracts/infra-outputs.yaml` 존재 및 `status: completed`
- **담당:** @Backend-Engineer-Agent (서비스별 병렬 호출 가능)
- **지침:** `services/telemetry/CLAUDE.md`, `services/alert/CLAUDE.md`, `services/legacy-sync/CLAUDE.md` 각각 전달
- **완료 조건:** `contracts/api-specs/*.yaml` 및 `contracts/kafka-schemas/*.yaml` 전부 생성 확인
- **사람에게 보고할 내용:** 생성된 API 엔드포인트 목록, 단위 테스트 커버리지
- **⛔ 사람 승인 없이 Phase 3 진행 금지**

### Phase 3 — CI/CD Pipeline Setup
- **선행 조건:** `contracts/api-specs/` 하위 파일 모두 존재
- **담당:** @DevOps-Engineer-Agent
- **지침:** `cicd/CLAUDE.md` 전달
- **완료 조건:** `.github/workflows/` 파이프라인 파일 생성 확인
- **사람에게 보고할 내용:** 파이프라인 구성 요약, 배포 전략
- **⛔ 사람 승인 없이 Phase 4 진행 금지**

### Phase 4 — Load Testing & Tuning
- **선행 조건:** CI/CD 파이프라인 정상 동작 확인
- **담당:** @DevOps-Engineer-Agent
- **지침:** `cicd/CLAUDE.md` 전달 (load-testing 섹션 참조 명시)
- **완료 조건:** JMeter 결과 보고서 생성
- **사람에게 보고할 내용:** TPS, p95 Latency, 에러율 수치 및 성공 기준 달성 여부

---

## contracts/ 운용 규칙
- 오케스트레이터는 contracts/ 파일의 **존재 여부와 status 필드만** 확인합니다.
- 내용 작성은 담당 에이전트만 수행합니다.
- 스키마 형식: YAML (각 contracts/ 파일의 주석 참조)

---

## Git 형상관리 운용 절차

> **전제:** 모든 규칙의 근거는 `LAWS.md Category 5`를 따릅니다.

### 브랜치 전략

```
main
├── phase/1-infra          ← @System-Architect-Agent 전담
├── phase/2-services       ← @Backend-Engineer-Agent 전담 (병렬 작업 가능)
│   ├── feat/telemetry
│   ├── feat/alert
│   └── feat/legacy-sync
├── phase/3-cicd           ← @DevOps-Engineer-Agent 전담
└── phase/4-load-test      ← @DevOps-Engineer-Agent 전담
```

- 각 에이전트는 자신의 **전담 브랜치에서만** 작업합니다.
- Phase 2는 서비스별 `feat/` 브랜치를 생성 후 `phase/2-services`로 병합합니다.

### 페이즈별 커밋 체크포인트

| 페이즈 | 브랜치 | 커밋 타이밍 | 커밋 scope |
|--------|--------|-------------|------------|
| Phase 1 | `phase/1-infra` | `contracts/infra-outputs.yaml` status: completed 직후 | `infra`, `contracts` |
| Phase 2 | `feat/{서비스명}` → `phase/2-services` | `contracts/api-specs/*.yaml` 및 kafka-schemas 전부 생성 직후 | `telemetry`, `alert`, `legacy-sync`, `contracts` |
| Phase 3 | `phase/3-cicd` | `.github/workflows/` 파일 생성 직후 | `ci`, `cicd` |
| Phase 4 | `phase/4-load-test` | JMeter 결과 보고서 생성 직후 | `test`, `load-test` |

### PR 병합 절차

1. 각 페이즈 브랜치 작업 완료 → 오케스트레이터가 사람에게 보고
2. **사람 승인** 후 PR 생성 (`phase/{번호}` → `main`)
3. PR 제목 형식: `[Phase {번호}] {페이즈 명칭} 완료`
4. Squash merge 사용 (페이즈 단위 히스토리 유지)
5. merge 완료 후 페이즈 브랜치 삭제

### 오케스트레이터 보고 시 포함 항목 (Git)

사람에게 페이즈 완료를 보고할 때 아래 항목을 함께 전달합니다:
- 브랜치명 및 최신 커밋 해시
- 커밋 수 및 변경 파일 목록 요약
- PR 생성 준비 완료 여부
