# CLAUDE.md — @System-Architect-Agent

> **⚠️ 작업 시작 전 `/LAWS.md`를 반드시 읽을 것. LAWS.md는 이 파일보다 우선한다.**

## 역할
AWS 인프라를 Terraform으로 프로비저닝합니다.
작업 완료 후 반드시 `contracts/infra-outputs.yaml`을 작성합니다.

---

## 참조 문서 (작업 전 반드시 읽을 것)
- `/ARCHITECTURE.md` — 전체 시스템 구성 및 AWS 서비스 목록
- `/SECURITY.md` — 네트워크 보안, 인증 방식, 암호화 요구사항
- `/RELIABILITY.md` — Multi-AZ, Auto-scaling 기준 (CPU 70% 초과 시 1분 내 Scale-out)
- `/QUALITY_SCORE.md` — tfsec 'High' 등급 취약점 제로 조건

---

## 구현 범위 (Phase 1)

### Networking
- VPC + Public/Private Subnet 분리 (SECURITY.md 기준 준수)
- API Gateway만 Public Subnet, App/DB는 Private Subnet
- AWS WAF 적용

### Compute
- AWS EKS 클러스터 (Multi-AZ, 최소 2개 AZ)
- HPA(Horizontal Pod Autoscaler): CPU 70% 초과 시 트리거

### Message Broker
- Amazon MSK (Managed Kafka)
- 토픽 생성: `telemetry-raw`, `telemetry-processed`, `alert-events`, `legacy-sync-queue`, `dead-letter-topic`

### Data Layer
- Amazon DynamoDB (테이블: `battery-telemetry`, PK: battery_id, SK: timestamp)
- Amazon RDS PostgreSQL (Multi-AZ, `vehicles` 및 `batteries` 테이블)
- Amazon S3 버킷 + Athena (QuickSight 연동용)

### Security
- AWS Secrets Manager: IoT 단말기 API Key 저장
- AWS KMS: RDS, DynamoDB at-rest 암호화
- TLS 1.2+ 강제

---

## 품질 게이트
작업 완료 전 반드시 실행:
```bash
tfsec ./terraform
```
'High' 등급 취약점이 0개여야 합니다. 존재할 경우 수정 후 재검사.

---

## contracts/ 출력 규칙
작업 완료 시 `contracts/infra-outputs.yaml`을 아래 스키마에 맞게 작성합니다.
파일 내 주석을 참고하여 실제 프로비저닝된 값을 기입하세요.

---

## 금지 사항
- `services/`, `cicd/` 디렉토리 수정 금지
- `contracts/api-specs/`, `contracts/kafka-schemas/` 수정 금지
- `terraform destroy` 실행 금지 (승인 없이)
