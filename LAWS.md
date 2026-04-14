# LAWS.md — 시스템 불변 법률

모든 에이전트는 역할에 관계없이 이 법률을 최우선으로 준수해야 합니다.
CLAUDE.md의 지침과 충돌할 경우 **LAWS.md가 항상 우선합니다.**

---

## Category 1. 보안 법률 (Security Laws)

### SEC-01: 시크릿 하드코딩 절대 금지
어떤 파일에도 API Key, 비밀번호, 토큰, ARN 등 민감한 값을 직접 작성하지 않는다.
- **허용:** 환경변수 참조 (`${VAR_NAME}`), AWS Secrets Manager ARN 참조
- **위반 예시:** `password: "battery1234"`, `api_key: "sk-abc123"`
- **위반 시:** 즉시 작업 중단 → 해당 값 제거 → 사람에게 보고

### SEC-02: Private Subnet 원칙 유지
App 서비스와 DB는 반드시 Private Subnet에만 배치한다.
API Gateway만 Public Subnet에 허용한다.
- **위반 예시:** DynamoDB, RDS, EKS Pod를 Public IP로 노출
- **위반 시:** 즉시 작업 중단 → 사람에게 보고

### SEC-03: TLS 강제
모든 서비스 간 통신은 TLS 1.2 이상을 적용한다.
- **예외:** docker-compose 로컬 환경 내부 통신 (동일 Docker 네트워크)
- **위반 예시:** 프로덕션 환경에서 `http://` 엔드포인트 사용, Kafka PLAINTEXT 단독 사용

### SEC-04: 최소 권한 원칙
IAM Role, Security Group은 해당 서비스가 실제로 필요한 권한만 부여한다.
- **위반 예시:** `"Action": "*"`, `"Resource": "*"` 의 와일드카드 남용
- **위반 시:** 즉시 수정 후 재검토

### SEC-05: tfsec High 등급 블로킹
Terraform 코드에 tfsec 'High' 등급 취약점이 존재하면 배포를 진행하지 않는다.
수정 없이 suppress 처리하는 것은 금지한다.

---

## Category 2. 데이터 무결성 법률 (Data Integrity Laws)

### DAT-01: Kafka 스키마 단방향 진화
`contracts/kafka-schemas/` 의 스키마는 하위 호환성을 깨는 변경을 할 수 없다.
필드 삭제 또는 타입 변경이 필요한 경우 반드시 사람의 승인을 받는다.
- **허용:** 새 optional 필드 추가
- **금지:** 기존 필드 삭제, 필드 타입 변경, required 필드 추가

### DAT-02: DB 스키마 직접 수정 금지
`batteries`, `vehicles` 테이블 구조 변경은 반드시 마이그레이션 파일(SQL)을 통해서만 수행한다.
`ALTER TABLE` 을 애플리케이션 코드나 Terraform에서 직접 실행하지 않는다.

### DAT-03: Idempotency 필수
모든 메시지 처리 로직은 동일한 메시지를 2회 이상 수신해도 데이터가 중복 생성되지 않도록 구현해야 한다.
- **기준 키:** `battery_id + timestamp` (Telemetry), `alert_id` (Alert), `sync_batch_id` (LegacySync)

### DAT-04: Dead Letter Topic 의무 사용
Kafka 메시지 처리가 3회 연속 실패할 경우 반드시 `dead-letter-topic`으로 전송한다.
메시지를 조용히 버리는(silent drop) 것은 금지한다.

### DAT-05: 데이터 삭제 작업 사람 승인 필수
DynamoDB, RDS의 레코드 또는 테이블 삭제, S3 버킷 삭제, `terraform destroy` 실행은
반드시 사람의 명시적 승인 후에만 실행한다.

---

## Category 3. 에이전트 월권 법률 (Agent Boundary Laws)

### AGT-01: 담당 디렉토리 외 수정 금지
각 에이전트는 자신이 담당하는 디렉토리만 수정한다.

| 에이전트 | 수정 가능 범위 |
|---|---|
| @System-Architect-Agent | `infra/` |
| @Backend-Engineer-Agent | `services/{담당서비스}/` |
| @DevOps-Engineer-Agent | `cicd/`, `.github/workflows/` |
| Orchestrator | 없음 (읽기 전용) |

### AGT-02: contracts/ 소유권 원칙
각 contracts/ 파일은 지정된 에이전트만 작성(write)할 수 있다.
다른 에이전트는 읽기(read)만 허용한다.

| 파일 | 작성 권한 |
|---|---|
| `contracts/infra-outputs.yaml` | @System-Architect-Agent |
| `contracts/kafka-schemas/telemetry-event.yaml` | @Backend(Telemetry) |
| `contracts/kafka-schemas/alert-event.yaml` | @Backend(Alert) |
| `contracts/kafka-schemas/legacy-sync-event.yaml` | @Backend(LegacySync) |
| `contracts/api-specs/*.yaml` | @Backend(담당 서비스) |

### AGT-03: contracts/ status 필드 역행 금지
`status: completed` 로 변경된 contracts/ 파일을 `status: pending` 으로 되돌리는 것은
사람의 승인 없이 불가하다.

### AGT-04: 오케스트레이터 코드 작성 금지
Orchestrator는 어떠한 소스 코드, Terraform, YAML, SQL도 직접 작성하지 않는다.
오직 서브에이전트 호출, 상태 확인, 사람에게 보고만 수행한다.

### AGT-05: 페이즈 선행 조건 검증 의무
각 페이즈 시작 전 선행 조건(contracts/ status 등)을 반드시 확인한다.
선행 조건 미충족 시 해당 페이즈를 시작하지 않고 오케스트레이터에게 보고한다.

---

## Category 4. 에스컬레이션 법률 (Escalation Laws)

아래 상황 발생 시 에이전트는 **즉시 작업을 중단**하고 사람에게 보고한다.
보고 없이 우회하거나 임의로 판단하여 진행하는 것은 금지한다.

### ESC-01: 보안 취약점 발견
tfsec High 등급 취약점, 하드코딩된 시크릿, SEC 법률 위반 가능성 발견 시.

### ESC-02: 품질 기준 미달
단위 테스트 커버리지 80% 미달, SonarQube A 등급 미달 상태에서 다음 단계 진행 불가.

### ESC-03: contracts/ 스키마 충돌
두 에이전트가 동일 contracts/ 파일의 스키마에 대해 서로 다른 해석을 가질 경우.

### ESC-04: 부하 테스트 SLA 미달
10,000 TPS 환경에서 p95 Latency 500ms 초과 또는 에러율 0.1% 초과 시.
임의로 테스트 기준을 낮추거나 결과를 조작하지 않는다.

### ESC-05: 예상치 못한 데이터 손실 위험
실행하려는 작업이 되돌릴 수 없는 데이터 손실을 유발할 가능성이 있을 경우.

### ESC-06: 선행 페이즈 결과물 이상
이전 페이즈의 contracts/ 파일이 누락되었거나 값이 비어있는 경우.
임의로 값을 추측하거나 채우지 않는다.
