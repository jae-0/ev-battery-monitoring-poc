# 🔋 EV Battery Real-time Monitoring System
> **Cloud Migration & AI Harness Engineering PoC — 10,000 TPS 목표 아키텍처 설계**

온프레미스로 운영 중인 EV 폐배터리 실시간 관제 시스템을 AWS 클라우드 기반 MSA로 전환하는 PoC 프로젝트입니다.
인프라 설계, 컨테이너 오케스트레이션, CI/CD, 부하 테스트 전 과정을 직접 구축했으며, 특히 **AI 멀티 에이전트 기반의 하네스(Harness) 엔지니어링**을 도입하여 인프라 프로비저닝(IaC)과 백엔드 개발 환경을 100% 자동화하는 데 집중했습니다.

[![AWS](https://img.shields.io/badge/AWS-Infra-232F3E?logo=amazon-aws)](https://aws.amazon.com/)
[![Terraform](https://img.shields.io/badge/Terraform-IaC-623CE4?logo=terraform)](https://www.terraform.io/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-EKS-326CE5?logo=kubernetes)](https://kubernetes.io/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)

---

## 💡 실전 비즈니스 시나리오 (Business Use Case)
본 시스템은 다음과 같은 실제 물류 현장의 **'대규모 데이터 수집'**과 **'초실시간 위기 대응'** 상황을 해결하기 위해 설계되었습니다.

* **배경 (Situation):** 전국에서 5,000대의 EV 폐배터리 운송 트럭이 본사로 이동 중입니다. 각 트럭에는 여러 개의 폐배터리가 실려 있으며, IoT 센서가 **1초마다 2건씩(총 10,000 TPS)** 배터리의 온도, 전압, GPS 위치 데이터를 클라우드로 쏘아 올립니다.
* **위기 발생 (Problem):** 고속도로를 달리는 104번 트럭 적재함에 있는 'BAT-99' 배터리의 온도가 결함으로 인해 갑자기 65°C를 돌파하며 급상승하기 시작합니다.
* **시스템의 해결 (Action):**
  1.  **[수집 & 버퍼링]** `Telemetry Service`는 초당 1만 건씩 쏟아지는 정상 트래픽 속에서도 시스템 다운 없이 이 데이터를 받아 **Kafka(MSK)**에 안전하게 버퍼링합니다.
  2.  **[실시간 탐지]** `Alert Service`가 Kafka 스트림을 즉시 소비하여, 500ms 이내에 '온도 임계치 초과(CRITICAL)'를 탐지합니다.
  3.  **[긴급 조치]** 시스템이 중앙 관제 센터에 경고를 띄우고, 104번 트럭 기사에게 "화재 위험! 즉시 정차 후 소화 준비" 긴급 PUSH 알림을 발송합니다.
  4.  **[레거시 동기화]** 이 모든 위기 대응 기록과 텔레메트리 데이터는 `Legacy Sync Service`를 통해 기존 온프레미스 사내망(PostgreSQL)으로 1시간 뒤 안전하게 요약 전송되어, 향후 해당 배터리의 상태 평가 증빙 자료로 쓰입니다.

---

## 🤖 AI Harness Engineering (인프라 자동화 및 개발 지원)
백엔드 개발팀이 비즈니스 로직에만 집중할 수 있도록, AI 에이전트(Architect, DevOps)를 활용한 **선언적 인프라 자동화 및 DevEx(Developer Experience) 환경**을 구축했습니다.

* **AI 친화적 IaC 설계 (Flat Terraform Structure):** AI의 컨텍스트 누락(Hallucination)을 방지하기 위해 복잡한 모듈화를 배제하고 `/infra/terraform` 내 단일 계층(Flat)으로 코드를 구성하여 프로비저닝 안정성을 극대화했습니다.
* **상태 기반 오케스트레이션:** `contracts/` 디렉토리 내 YAML 파일 상태를 기반으로 인프라 배포 결과(엔드포인트, 인증 정보)가 백엔드 서비스 환경 변수로 자동 주입되도록 파이프라인을 설계했습니다.
* **로컬 DevEx 완벽 지원:** 클라우드 비용 없이도 개발자가 로컬에서 전체 인프라(Kafka, DynamoDB, RDS)를 모사하여 테스트할 수 있도록 Docker Compose 기반의 원클릭 샌드박스를 제공합니다.

---

## 🎯 성능 목표 및 달성 지표

| 지표 | 목표 (AWS 환경) | 측정 결과 (Local Docker 환경) |
|------|-----------------|-------------------------------|
| **처리량 (Throughput)** | 10,000 TPS | 155.9 TPS (로컬 단일 PC 제약) |
| **응답시간 (Latency)** | p95 500ms 미만 | 1,417ms (DynamoDB Local I/O 병목) |
| **신뢰성 (Reliability)** | 에러율 0.1% 미만 | **0.00% (목표 달성 완료)** |

> *참고: 로컬 환경은 JMeter·앱·인프라 컨테이너가 단일 PC 리소스를 경합하여 성능 제약이 발생합니다. TPS/Latency 목표는 AWS 분산 환경 기준이며, 아키텍처(EKS HPA + MSK + DynamoDB)는 해당 목표를 달성하도록 설계되었습니다.*

---

## 🏗️ 인프라 아키텍처

```text
                        ┌─────────────────────────────────────────────┐
                        │                   AWS Cloud                  │
                        │                                              │
  IoT 단말기             │  ┌──────────┐      ┌─────────────────────┐  │
  (배터리 센서) ─────────┼─▶│   API    │─────▶│    EKS Cluster      │  │
                        │  │ Gateway  │      │                     │  │
                        │  │  + WAF   │      │  ┌───────────────┐  │  │
                        │  └──────────┘      │  │Telemetry Svc  │  │  │
                        │                    │  └──────┬────────┘  │  │
                        │  ┌──────────────┐  │         │ Kafka     │  │
                        │  │    Amazon    │◀─┼─────────┤           │  │
                        │  │    MSK       │  │         ▼           │  │
                        │  │  (Kafka)     │  │  ┌───────────────┐  │  │
                        │  └──────────────┘  │  │  Alert Svc    │  │  │
                        │                    │  └───────────────┘  │  │
                        │  ┌──────────────┐  │                     │  │
                        │  │  DynamoDB    │◀─┼─ ┌───────────────┐  │  │
                        │  │  (telemetry) │  │  │Legacy Sync Svc│  │  │
                        │  └──────────────┘  │  └───────────────┘  │  │
                        │                    └─────────────────────┘  │
                        │  ┌──────────────┐                           │
                        │  │  RDS (PG)    │   Private Subnet only     │
                        │  │  Multi-AZ    │                           │
                        │  └──────────────┘                           │
                        └─────────────────────────────────────────────┘
```

---

## 🛠️ 기술 스택

| 영역 | 기술 |
|------|------|
| **IaC & Orchestration** | Terraform, AWS EKS, Docker |
| **Message Broker** | Amazon MSK (Apache Kafka) |
| **Data Storage** | Amazon DynamoDB (Real-time), RDS PostgreSQL 15 (Master), S3 (아티팩트) |
| **CI/CD & Automation** | GitHub Actions |
| **Security** | AWS WAF, Secrets Manager, KMS |
| **Code Quality** | SonarQube (Quality Gate), JaCoCo (커버리지 80% 게이트) |
| **Observability** | Prometheus, Grafana, CloudWatch |
| **Load Test** | JMeter (Docker CLI) |
| **Application** | Java 17, Spring Boot 3.2 |

---

## 📂 프로젝트 구조

```text
.
├── .github/
│   └── workflows/        # GitHub Actions 파이프라인 (build-and-test.yml, deploy.yml)
├── infra/
│   ├── terraform/        # AI 친화적 Flat 구조 IaC
│   │   ├── vpc.tf        # 네트워크 (VPC, Subnet, NAT GW)
│   │   ├── eks.tf        # EKS 클러스터 및 워커 노드 그룹
│   │   ├── msk.tf        # Amazon MSK (Kafka)
│   │   ├── dynamodb.tf   # DynamoDB 테이블
│   │   ├── rds.tf        # RDS PostgreSQL Multi-AZ
│   │   ├── s3.tf         # S3 버킷 (아티팩트 저장)
│   │   ├── security.tf   # IAM, WAF, Security Group
│   │   ├── providers.tf  # AWS Provider 및 버전 고정
│   │   ├── variables.tf  # 입력 변수 정의
│   │   └── outputs.tf    # 배포 결과 출력값 (contracts/ 주입용)
│   ├── k8s/              # Kubernetes 매니페스트 (Namespace, Deployment, HPA)
│   └── local/            # 로컬 DB 초기화 스크립트 (init.sql)
├── services/             # MSA 백엔드 소스코드 (Telemetry, Alert, Legacy-Sync)
├── load-test/            # JMeter 시나리오 및 실행 스크립트
│   ├── glovis-poc.jmx          # 10,000 TPS 정식 시나리오 (RELIABILITY.md 기준)
│   ├── telemetry-load-test.jmx # 로컬 빠른 검증용 시나리오
│   └── run-load-test.sh        # Docker JMeter 실행 스크립트
├── contracts/            # AI 에이전트 간 작업 상태 및 API/Kafka 스키마 명세
├── cicd/                 # DevOps 에이전트 지침 (CLAUDE.md)
├── docs/                 # 설계 문서, 기술 부채, 스펙 문서
├── references/           # 외부 참고 자료
└── docker-compose.yml    # 개발팀을 위한 로컬 인프라 대체 샌드박스 (DevEx)
```

---

## ⚙️ 인프라 설계 상세

### 네트워크 및 보안 (VPC & Security)
- **Subnet 격리**: 외부 접근이 필요한 API/NAT Gateway만 Public 서브넷에 배치하고, EKS 워커 노드, MSK, RDS는 Private 서브넷에 철저히 격리.
- **L7 보안**: AWS WAF를 적용하여 비정상 트래픽 및 웹 취약점(SQLi, XSS) 사전 차단.
- **Secrets Management**: DB 자격증명 및 API Key는 AWS Secrets Manager를 통해 동적 주입하여 소스코드 내 하드코딩 원천 차단.

### Kubernetes (EKS) 및 가용성
- **오토스케일링**: HPA(Horizontal Pod Autoscaler)를 적용하여 파드 CPU 사용률 70% 초과 시 즉각적인 Scale-out 트리거.
- **Fault Tolerance**: RDS Multi-AZ 페일오버 구성 및 앱 단의 Circuit Breaker(Resilience4j) 적용으로 장애 전파 차단.
- **데이터 유실 방지**: Kafka 내 `dead-letter-topic`을 구성하여 3회 이상 처리 실패한 이상 메시지를 격리 및 후속 재처리.

---

## 🚀 Deployment & Operations

### 1. 로컬 샌드박스 (Docker Compose)
클라우드 배포 전, 개발자가 로컬에서 완벽하게 통합 테스트를 수행할 수 있는 환경입니다.
```bash
# 전체 인프라(Kafka, DB Local) + 서비스 실행 (의존성 순서 자동 관리)
docker compose up -d

# 로그 모니터링 (이상 탐지 시뮬레이션)
docker compose logs alert-service -f
```

### 2. AWS 프로비저닝 (Terraform)
```bash
cd infra/terraform
terraform init
terraform apply -var-file="terraform.tfvars"
```

### 3. CI/CD 파이프라인 (GitHub Actions)

**[build-and-test.yml]** `Push/PR` ➡️ `Maven Test (JaCoCo 80% Gate)` ➡️ `SonarQube Quality Gate` ➡️ `Docker Build` ➡️ `tfsec (infra/ 변경 시)`

**[deploy.yml]** `main 머지` ➡️ `ECR Push` ➡️ `Deploy to EKS (Rolling Update)` ➡️ `Rollout 검증 (실패 시 자동 롤백)`

---

## 💡 주요 트러블슈팅 (Troubleshooting)
인프라 구성 및 로컬 하네스 샌드박스 구축 과정에서 마주한 주요 이슈와 해결 과정입니다.

### 인프라 & 컨테이너 환경
**1. ZooKeeper 헬스체크 지속 실패**
* **원인**: Confluent ZK 이미지에 `nc`(netcat) 패키지가 누락되어 Docker 헬스체크 스크립트 실패.
* **해결**: `nc` 대신 이미지에 내장된 `cub zk-ready localhost:2181 10` 명령어로 헬스체크 구문 교체.

**2. DynamoDB Local 볼륨 퍼미션 오류 (AccessDeniedException)**
* **원인**: 컨테이너 내부 유저 권한과 호스트 마운트 볼륨(`/data`)의 소유권 불일치.
* **해결**: 영속성이 필요 없는 로컬 개발 환경 특성을 고려, 마운트 대신 `-inMemory` 모드로 실행하여 I/O 병목 및 권한 문제 동시 해결.

**3. Kubernetes 파드에서 로컬 Kafka 연결 실패 (Connection refused)**
* **원인**: Kafka가 `localhost:9092`로 Advertise하여, K8s 파드가 이를 해석할 때 자신의 로컬호스트로 라우팅됨.
* **해결**: Kafka `KAFKA_ADVERTISED_LISTENERS` 설정을 `host.docker.internal`로 변경하고 컨테이너 완전 재생성(`up -d`).

**4. K8s 로컬 배포 시 ErrImageNeverPull 에러**
* **원인**: Docker Desktop K8s의 containerd 런타임이 로컬 캐시의 이미지를 직접 참조하지 못함.
* **해결**: 로컬 레지스트리 컨테이너(`localhost:5000`)를 띄우고 이미지를 Push한 뒤 매니페스트에서 해당 주소를 참조하도록 파이프라인 수정.

### 애플리케이션 & 프레임워크 연동
**5. @Configuration 내 @PostConstruct 실행 순서 문제 (NPE 발생)**
* **원인**: `DynamoDbClient` 빈이 생성되기 전에 `@PostConstruct` 테이블 생성 로직이 먼저 실행되어 `NullPointerException` 발생.
* **해결**: 테이블 초기화 로직을 별도의 `@Component` 빈(`DynamoDbTableInitializer`)으로 분리하여 의존성 주입 생명주기 안정화.

**6. Kafka 메시지 역직렬화 실패 (ClassNotFoundException)**
* **원인**: 송신측(Telemetry) 패키지 경로가 포함된 `__TypeId__` 헤더를 수신측(Alert)이 그대로 읽어 자신의 클래스패스에서 찾지 못함.
* **해결**: 수신측 Kafka Consumer 설정에서 `spring.json.use.type.headers: false` 처리 및 기본 매핑 클래스 명시.

**7. Spring Boot 3.2 RestTemplateBuilder 타임아웃 API 변경**
* **원인**: Boot 3.2부터 `connectTimeout(Duration)` 메서드가 제거되어 빌드 에러 발생.
* **해결**: `SimpleClientHttpRequestFactory`를 직접 인스턴스화하여 타임아웃 세팅 후 `RestTemplate`에 주입하는 방식으로 마이그레이션.

**8. Java 17 + Docker 환경에서 Mockito Self-attach 실패**
* **원인**: Java 17의 엄격해진 보안 정책과 Docker 컨테이너의 제한된 환경이 맞물려 JVM self-attach 차단됨.
* **해결**: Maven Surefire 플러그인에 `-Djdk.attach.allowAttachSelf=true` JVM 옵션 추가 및 Mockito extension(`mock-maker-subclass`) 설정 추가.

**9. Mockito UnfinishedStubbingException 발생**
* **원인**: `willReturn()` 파라미터 내부에서 또 다른 `mock()` 객체를 생성/호출하여 Mockito의 내부 Stubbing 상태 관리 충돌.
* **해결**: Mock 객체 생성(Arrange)과 Stubbing(Act) 단계를 명확히 분리하여 변수 할당 후 `given()` 파라미터로 전달.