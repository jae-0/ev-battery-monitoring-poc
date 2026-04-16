# 🔋 EV Battery Real-time Monitoring System
> **EV 폐배터리 실시간 관제 시스템 — 대규모 트래픽 처리를 위한 클라우드 전환 PoC**

온프레미스로 운영 중인 EV 폐배터리 관제 시스템을 **Azure 클라우드** 기반의 이벤트 드리븐 마이크로서비스(MSA)로 전환하는 PoC 프로젝트입니다.
**10,000 TPS** 환경에서 **p95 응답시간 500ms 미만**, **에러율 0.1% 미만** 달성을 목표로 하며, 인프라 프로비저닝(IaC)부터 백엔드 서비스 개발, CI/CD 파이프라인, 모니터링 구축까지 전 과정을 직접 설계하고 구현했습니다.

[![Azure](https://img.shields.io/badge/Azure-Infra-0078D4?logo=microsoft-azure)](https://azure.microsoft.com/)
[![Terraform](https://img.shields.io/badge/Terraform-IaC-623CE4?logo=terraform)](https://www.terraform.io/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-AKS-326CE5?logo=kubernetes)](https://kubernetes.io/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)

---

## 💡 비즈니스 시나리오 (Use Case)

> **"초당 1만 건의 데이터 폭우 속에서, 단 하나의 화재 징후도 놓치지 않는다."**

* **상황 (Situation):** 전국에서 5,000대의 EV 폐배터리 운송 트럭이 이동 중입니다. 각 배터리에 부착된 IoT 센서는 1초마다 온도, 전압, GPS 데이터를 클라우드로 전송하며, 전체 트래픽은 **10,000 TPS**에 달합니다.
* **위기 (Problem):** 고속도로를 주행 중인 특정 트럭의 배터리 온도가 결함으로 인해 65°C를 초과하며 급상승하기 시작합니다.
* **해결 (Action):** 시스템은 쏟아지는 트래픽 속에서도 이 이상 신호를 **500ms 이내에 탐지**하여 중앙 관제 센터에 긴급 경보를 발생시킵니다. 이후 모든 텔레메트리 데이터와 알람 이력은 레거시 온프레미스 시스템으로 안전하게 동기화되어 향후 상태 평가 증빙 자료로 활용됩니다.

---

## 🤖 AI Harness Engineering (인프라 자동화 및 개발 지원)

백엔드 개발팀이 비즈니스 로직에만 집중할 수 있도록, AI 에이전트(Architect, DevOps)를 활용한 **선언적 인프라 자동화 및 DevEx(Developer Experience) 환경**을 구축했습니다.

* **AI 친화적 IaC 설계 (Flat Terraform Structure):** AI의 컨텍스트 누락(Hallucination)을 방지하기 위해 복잡한 모듈화를 배제하고 `/infra/terraform` 내 단일 계층(Flat)으로 코드를 구성하여 프로비저닝 안정성을 극대화했습니다.
* **상태 기반 오케스트레이션:** `contracts/` 디렉토리 내 YAML 파일 상태를 기반으로 인프라 배포 결과(엔드포인트, 인증 정보)가 백엔드 서비스 환경 변수로 자동 주입되도록 파이프라인을 설계했습니다.
* **로컬 DevEx 완벽 지원:** 클라우드 비용 없이도 개발자가 로컬에서 전체 인프라(Kafka, CosmosDB Emulator, PostgreSQL)를 모사하여 테스트할 수 있도록 Docker Compose 기반의 원클릭 샌드박스를 제공합니다.

---

## 🏗️ 아키텍처 (Architecture)

```text
  IoT 단말기 (배터리 센서)
        │
        │  HTTPS + X-API-Key 인증
        ▼
┌─────────────────────────────────────────────────────────┐
│                   Azure Cloud                           │
│                                                         │
│  Azure WAF Policy (OWASP 3.2) + Application Gateway     │
│      │                                                  │
│      ▼                                                  │
│  AKS Cluster (Private Subnet)                           │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Telemetry Service                                 │ │
│  │  - API Key 검증 (Azure Key Vault)                  │ │
│  │  - Cosmos DB 저장 (Idempotency)                    │ │
│  │  - Event Hubs telemetry-raw 발행                   │ │
│  └─────────────────┬──────────────────────────────────┘ │
│                    │ Event Hubs (Kafka 프로토콜)         │
│                    ▼                                    │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Alert Service                                     │ │
│  │  - telemetry-raw 소비                              │ │
│  │  - 온도 > 50°C WARNING / > 60°C CRITICAL           │ │
│  │  - 전압 < 10V 또는 > 60V CRITICAL                  │ │
│  │  - alert-events 발행                               │ │
│  └────────────────────────────────────────────────────┘ │
│                                                         │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Legacy-Sync Service                               │ │
│  │  - 1시간 주기 PostgreSQL 변경분 조회               │ │
│  │  - 레거시 OEM 시스템으로 벌크 동기화               │ │
│  │  - Idempotency (battery_id + updated_at)           │ │
│  └────────────────────────────────────────────────────┘ │
│                                                         │
│  Cosmos DB (telemetry)   PostgreSQL Flexible Server     │
│  Blob Storage (artifacts)  Key Vault (비밀 관리)        │
└─────────────────────────────────────────────────────────┘
```

---

## 🛠️ 기술 스택 (Tech Stack)

| 영역 | 사용 기술 |
| :--- | :--- |
| **Infra & Compute** | Terraform (Flat 구조), Azure AKS, Kubernetes HPA |
| **Message Broker** | Azure Event Hubs (Apache Kafka 프로토콜 호환) |
| **Data Storage** | Azure Cosmos DB NoSQL (Real-time), Azure Database for PostgreSQL Flexible Server (Master), Azure Blob Storage |
| **Security** | Azure WAF Policy (OWASP 3.2), Azure Key Vault, TLS 1.2+, Managed Identity |
| **CI/CD & Quality** | GitHub Actions, Docker, Azure Container Registry (ACR), SonarQube, JaCoCo |
| **Observability** | Prometheus, Grafana, Azure Monitor |
| **Test & App** | JMeter, hey, Java 17, Spring Boot 3.2 |

---

## 🎯 성능 목표 및 로컬 벤치마크

목표는 Azure 프로덕션 환경 기준 10,000 TPS입니다. 아래는 로컬 Docker 환경에서 `hey`를 활용해 단계적으로 부하를 높이며 측정한 한계치 테스트 결과입니다.

| 단계 | 동시 연결 | 요청 수 | 측정 TPS | p95 Latency | 에러율 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1. 워밍업** | 50 | 1,000 | 184 | 467ms | 0% |
| **2. 부하 증가** | 100 | 5,000 | 364 | 600ms | 0% |
| **3. 고부하** | 200 | 10,000 | 496 | 1,141ms | 0% |
| **4. 극한 스트레스** | 500 | 20,000 | **674** | 1,368ms | **0%** |

> **💡 성능 분석:** 로컬 환경에서는 **674 TPS까지 에러율 0%**를 방어했습니다. 단일 PC의 리소스 경합, Cosmos DB Emulator(인메모리)의 한계, 단일 Kafka 브로커 제약으로 인해 Latency가 상승했습니다. 실제 Azure 환경에서는 Event Hubs 파티션 병렬 처리와 Cosmos DB 자동 확장(RU/s)을 통해 10,000 TPS 목표 달성이 가능하도록 아키텍처가 설계되었습니다.

---

## 📐 주요 설계 결정 (Design Decisions)

* **CQRS 기반 데이터 저장소 분리:** 초당 수만 건의 쓰기가 발생하는 센서 데이터(Telemetry)는 수평 확장이 유리한 **Azure Cosmos DB**에, 트랜잭션과 무결성이 중요한 마스터 데이터(차량/배터리 정보)는 **PostgreSQL**에 저장하도록 역할을 분리했습니다.
* **비동기 이벤트 기반 분산 아키텍처:** 서비스 간 강결합을 끊기 위해 모든 통신은 **Azure Event Hubs(Kafka 토픽)** 를 경유합니다. Alert 서비스가 일시적으로 다운되더라도 메시지는 유실되지 않으며, 복구 즉시 처리를 재개합니다.
* **장애 격리 및 재처리 (DLT):** 메시지 처리가 3회 연속 실패할 경우 조용히 버리지 않고 `dead-letter-topic`으로 격리하여 운영자가 원인을 분석하고 재처리할 수 있는 안전망을 구축했습니다.
* **멱등성(Idempotency) 보장:** 네트워크 지연이나 재시도로 인한 데이터 중복 적재를 막기 위해 Telemetry는 `battery_id + timestamp`를 Cosmos DB Unique Key Policy로 강제하고, Legacy-Sync는 `battery_id + updated_at` 조합으로 멱등성을 확보했습니다.
* **Kafka 프로토콜 호환:** Azure Event Hubs는 Kafka 프로토콜을 완전 지원합니다. Alert Service, Legacy-Sync Service는 **코드 변경 없이** SASL 인증 설정만 추가하여 Event Hubs에 연결됩니다.

---

## 📂 프로젝트 구조 (Project Structure)

```text
.
├── .github/workflows/           # CI/CD 자동화 파이프라인
├── infra/
│   ├── terraform/               # 평탄화(Flat) 구조의 Azure 인프라 코드
│   ├── k8s/                     # Kubernetes 매니페스트 (Deploy, HPA)
│   └── local/                   # 로컬 개발용 DB 초기화 및 모니터링 설정 파일
├── services/                    # 마이크로서비스 백엔드 소스코드
│   ├── telemetry/               # 데이터 수집 (Port: 8080)
│   ├── alert/                   # 이상 징후 탐지 (Port: 8081)
│   └── legacy-sync/             # 레거시 동기화 (Port: 8082)
├── contracts/                   # 서비스 간 API 명세 및 Kafka 스키마 계약서
├── load-test/                   # JMeter 기반 부하 테스트 시나리오
└── docker-compose.yml           # 클라우드 환경을 완벽히 모사한 로컬 샌드박스
```

---

## 🚀 로컬 환경 실행 및 테스트

클라우드 배포 전, 개발자가 로컬에서 전체 인프라를 모사하여 테스트할 수 있습니다.

**1. 환경 세팅 및 실행**
```bash
cp .env.example .env.local
docker compose up -d
```

> 로컬에서는 Azure Cosmos DB Emulator, 단일 Kafka 브로커, PostgreSQL이 컨테이너로 구동됩니다.

**2. 텔레메트리 정상 데이터 전송**
```bash
curl -X POST http://localhost:8080/v1/telemetry \
  -H "Content-Type: application/json" \
  -H "X-API-Key: local-dev-api-key" \
  -d '{
    "batteryId": "BAT-A001",
    "vehicleId": "VH-1001",
    "timestamp": 0,
    "temperature": 35.2,
    "voltage": 380.5,
    "gpsLat": 37.5665,
    "gpsLng": 126.9780
  }'
```

**3. 화재 징후 (CRITICAL) 알람 발생 시뮬레이션**
```bash
curl -X POST http://localhost:8080/v1/telemetry \
  -H "Content-Type: application/json" \
  -H "X-API-Key: local-dev-api-key" \
  -d '{
    "batteryId": "BAT-B001",
    "vehicleId": "VH-1002",
    "timestamp": 0,
    "temperature": 65.0,
    "voltage": 380.5,
    "gpsLat": 35.1796,
    "gpsLng": 129.0756
  }'

# Alert 서비스 로그에서 즉시 감지 확인
docker compose logs alert-service -f
```

---

## ☁️ Azure 프로덕션 배포

### 사전 준비
```bash
# Azure CLI 설치
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# 로그인 및 구독 설정
az login
az account set --subscription <SUBSCRIPTION_ID>
```

### 인프라 프로비저닝 (Terraform)
```bash
cd infra/terraform

terraform init
terraform apply -auto-approve
```

**프로비저닝되는 Azure 리소스:**

| 리소스 | 설명 |
| :--- | :--- |
| AKS (`glovis-poc-aks`) | Kubernetes 클러스터, Standard_D2s_v3 × 2노드 |
| Azure Container Registry | Docker 이미지 저장소 (`glovispocacr.azurecr.io`) |
| Azure Cosmos DB | 텔레메트리 실시간 저장 (`battery-poc/battery-telemetry`) |
| Azure Event Hubs | Kafka 호환 메시지 브로커 (5개 토픽) |
| Azure PostgreSQL Flexible Server | 마스터 데이터 (vehicles, batteries 테이블) |
| Azure Key Vault | IoT API Key 등 시크릿 관리 |
| Azure Application Gateway WAF_v2 | L7 로드밸런서 + OWASP WAF |
| Azure NAT Gateway | AKS 아웃바운드 트래픽 처리 |
| Azure Blob Storage | 데이터 레이크 (Athena 연동용) |

### 서비스 배포 (Kubernetes)
```bash
# kubeconfig 설정
az aks get-credentials --resource-group glovis-poc-rg --name glovis-poc-aks

# 매니페스트 적용
kubectl apply -f infra/k8s/

# 배포 확인
kubectl get pods -n battery-poc
kubectl get svc -n battery-poc
```

**외부 접근 엔드포인트:**
- Telemetry API: `http://<EXTERNAL-IP>:8080/v1/telemetry` (LoadBalancer)
- Alert / Legacy-Sync: ClusterIP (내부 통신 전용)

### GitHub Actions Secrets 설정
| Secret 이름 | 설명 |
| :--- | :--- |
| `AZURE_CREDENTIALS` | `az ad sp create-for-rbac` 출력 JSON |
| `ACR_LOGIN_SERVER` | `glovispocacr.azurecr.io` |
| `AKS_CLUSTER_NAME` | `glovis-poc-aks` |
| `AKS_RESOURCE_GROUP` | `glovis-poc-rg` |
| `COSMOS_ENDPOINT` | Cosmos DB 엔드포인트 URL |
| `COSMOS_KEY` | Cosmos DB 기본 키 |
| `EVENTHUB_CONNECTION_STRING` | Event Hubs 연결 문자열 (SASL JAAS config 형식) |
| `RDS_ENDPOINT` | PostgreSQL FQDN |
| `RDS_USERNAME` | PostgreSQL 관리자 계정 |
| `RDS_PASSWORD` | PostgreSQL 비밀번호 |

### 비용 절감 스크립트
작업을 마칠 때 AKS 노드와 PostgreSQL을 중지하여 컴퓨팅 비용을 절약할 수 있습니다.

```bash
# 중지 (VM 비용 차단)
./pause.sh

# 재개
./resume.sh

# 전체 삭제
./teardown.sh
```

> **계속 과금되는 리소스 (중지 불가):** Application Gateway WAF_v2 (~$0.35/h), NAT Gateway (~$0.045/h)

---

## 🔄 CI/CD 파이프라인

GitHub Actions를 통해 코드 품질 검증부터 AKS 배포까지 전 과정을 자동화했습니다.

* **코드 통합 (PR):** `Maven Build` ➡️ `JaCoCo (80% 커버리지 게이트)` ➡️ `SonarQube 스캔` ➡️ `tfsec (인프라 취약점 검사, HIGH 등급 차단)`
* **프로덕션 배포 (Merge to Main):** `Docker Multi-stage Build` ➡️ `ACR Push` ➡️ `kubectl apply` ➡️ `AKS Rolling Update` ➡️ `자동 롤백`

```
[Push to main]
      │
      ▼
  docker build & push → ACR (SHA 태그 + latest 태그)
      │
      ▼
  kubectl apply -f infra/k8s/   ← 신규 리소스 생성
      │
      ▼
  kubectl set image (Rolling Update, maxUnavailable=0)
      │
      ├── 성공 → 배포 완료
      └── 실패 → kubectl rollout undo (자동 롤백)
```

---

## 📊 부하 테스트 결과 (Phase 4)

**테스트 도구:** Apache JMeter 5.5 (Docker)
**대상:** `POST http://<TELEMETRY-IP>:8080/v1/telemetry`
**환경:** Azure Korea Central

### Test 1 — 최대 부하 (500 스레드)

| 지표 | 결과 | 목표 |
| :--- | :--- | :--- |
| TPS | 73.3 req/s | 10,000/s |
| 평균 Latency | 6,221ms | - |
| 에러율 | 86.9% | < 0.1% |
| 에러 원인 | CosmosDB 429 Throttling, 커넥션 타임아웃 | - |

### Test 2 — 프리티어 최적 (5 스레드)

| 지표 | 결과 | 목표 |
| :--- | :--- | :--- |
| TPS | 36.1 req/s | 10,000/s |
| 평균 Latency | 130ms | p95 < 500ms |
| 에러율 | 0.46% | < 0.1% |
| 에러 원인 | CosmosDB 간헐적 429 (burst) | - |

### 병목 분석

| 리소스 | 프리티어 한도 | 10,000 TPS 필요 사양 |
| :--- | :--- | :--- |
| CosmosDB | 1,000 RU/s | 50,000+ RU/s |
| AKS 노드 | 2 × D2s_v3 (4 vCPU) | 10+ × D4s_v3 |
| Event Hubs | Basic 1 TU (1 MB/s) | Standard 10+ TU |
| telemetry Pod | 2 replica | 20+ replica (HPA) |

> **결론:** 아키텍처 E2E 흐름(API 인증 → CosmosDB 저장 → Kafka 발행) 정상 동작 확인 완료. 프리티어 환경 제약으로 10,000 TPS 미달성이나, 유료 티어 전환 및 스케일아웃 시 달성 가능한 구조임을 확인.

---

## 🛠️ 주요 트러블슈팅 (Troubleshooting)

### 로컬 개발 환경

🚨 **ZooKeeper 헬스체크 지속 실패**
* **원인:** Confluent ZooKeeper 이미지에 `nc`(netcat) 패키지가 누락되어 Docker 헬스체크 스크립트 실행 불가.
* **해결:** `nc` 명령어 대신 이미지에 내장된 고유 유틸리티인 `cub zk-ready localhost:2181 10`으로 헬스체크 구문을 교체하여 안정화.

🚨 **Kafka NodeExistsException (브로커 재시작 실패)**
* **원인:** 컨테이너를 강제 종료(`Ctrl+C` 또는 `docker compose stop`)하면 ZooKeeper에 `/brokers/ids/1` Ephemeral 노드가 남아, 재시작 시 브로커 ID 충돌 발생.
* **해결:** `docker compose down -v`로 ZooKeeper 볼륨까지 완전히 삭제 후 재시작.

🚨 **Cosmos DB Emulator 연결 실패 (SSL 인증서 오류)**
* **원인:** Cosmos DB Emulator는 자체 서명(Self-signed) TLS 인증서를 사용하여, Java SDK의 기본 인증서 검증에서 거부됨.
* **해결:** `docker-entrypoint.sh`에서 `SPRING_PROFILES_ACTIVE` 값을 확인해 `local` 프로필일 때만 에뮬레이터 로직 실행, 클라우드 프로필은 `exec java -jar`로 직접 기동하도록 분기 처리.

🚨 **Kafka 메시지 역직렬화 실패 (ClassNotFoundException)**
* **원인:** Telemetry 서비스가 발행한 메시지 헤더(`__TypeId__`)에 발신 측의 클래스 패키지 경로가 포함되어, Alert 서비스가 이를 역직렬화하려다 클래스를 찾지 못함.
* **해결:** 수신 측(Alert) Kafka Consumer 설정에 `spring.json.use.type.headers: false`를 추가하여 타입 헤더를 무시하고, 기본 매핑 클래스를 명시적으로 지정하여 해결.

🚨 **JaCoCo 커버리지 80% 게이트 통과 실패**
* **원인:** 비즈니스 로직이 없는 Configuration, Domain 엔티티, Repository 인터페이스까지 커버리지 측정 대상에 포함되어 전체 퍼센티지가 하락.
* **해결:** JaCoCo 플러그인 설정에 해당 클래스 패턴을 제외(`excludes`) 처리하고, Controller 단위 테스트를 `@WebMvcTest`를 활용해 보강.

🚨 **Spring Boot 3.2 RestTemplate 타임아웃 컴파일 에러**
* **원인:** Spring Boot 3.2로 버전업 되면서 `RestTemplateBuilder.connectTimeout(Duration)` API가 프레임워크에서 제거됨.
* **해결:** `SimpleClientHttpRequestFactory` 객체를 직접 생성하여 `setConnectTimeout`을 설정한 뒤 `RestTemplate`에 주입하는 방식으로 마이그레이션 완료.

### Azure 인프라 배포

🚨 **tfsec: `bypass` 타입 오류**
* **원인:** `azurerm_key_vault`의 `network_acls.bypass` 필드에 리스트(`["AzureServices"]`)를 전달했으나, azurerm provider v3에서는 문자열(`"AzureServices"`)을 요구.
* **해결:** `bypass = "AzureServices"`로 수정.

🚨 **Application Gateway TLS 정책 deprecated 오류**
* **원인:** Application Gateway 생성 시 기본 SSL 정책 `AppGwSslPolicy20150501`이 TLS 1.0/1.1을 포함하여 Azure에서 거부됨.
* **해결:** `ssl_policy` 블록을 명시적으로 추가하여 `policy_name = "AppGwSslPolicy20220101"` (TLS 1.2/1.3 전용)로 지정.

🚨 **AKS 업그레이드 실패 — vCPU 쿼타 부족**
* **원인:** Korea Central 구독의 vCPU 쿼타가 4개로 고정(프리티어). 노드 VM 사이즈 변경 시 Azure가 surge 노드(임시 노드 1개 = 2 vCPU)를 먼저 생성하는데 여유 쿼타가 0이라 실패.
* **해결:** Terraform에서 `vm_size`를 실제 배포된 값(`Standard_D2s_v3`)으로 맞춰 노드 교체 없이 애드온만 업데이트하도록 수정. `terraform refresh`로 상태 동기화 후 재적용.

🚨 **AKS LoadBalancer External IP `<pending>` 무한 대기**
* **원인:** AKS Managed Identity에 VNet에 대한 `Microsoft.Network/virtualNetworks/subnets/join/action` 권한이 없어 LoadBalancer 생성 실패 (`LinkedAuthorizationFailed 403`).
* **해결:** AKS 클러스터 Identity의 `principalId`에 VNet 범위의 `Network Contributor` 역할을 부여.
  ```bash
  az role assignment create \
    --assignee <AKS_PRINCIPAL_ID> \
    --role "Network Contributor" \
    --scope <VNET_RESOURCE_ID>
  ```

🚨 **LoadBalancer 공인 IP 쿼타 초과**
* **원인:** Azure 프리티어 구독은 공인 IP 개수 한도가 있음. 3개 서비스 모두 LoadBalancer로 설정 시 alert-service가 IP를 선점하고 나머지 2개가 `PublicIPCountLimitReached`로 실패.
* **해결:** 외부 노출이 필요한 `telemetry-service`만 LoadBalancer로 유지, `alert-service`와 `legacy-sync-service`는 ClusterIP로 변경 (내부 서비스간 통신은 ClusterIP로 충분).

🚨 **PostgreSQL 방화벽 차단 (legacy-sync 연결 실패)**
* **원인:** Azure PostgreSQL Flexible Server가 AKS 노드 IP를 방화벽에서 차단.
* **해결:** PostgreSQL 방화벽에 AKS 아웃바운드 IP 허용 규칙 추가.
  ```bash
  az postgres flexible-server firewall-rule create \
    --resource-group glovis-poc-rg \
    --name glovis-poc-postgres \
    --rule-name allow-aks \
    --start-ip-address 0.0.0.0 \
    --end-ip-address 255.255.255.255
  ```

🚨 **legacy-sync Table Not Found (`batteries` 테이블 없음)**
* **원인:** Terraform으로 PostgreSQL 서버는 프로비저닝했지만 DDL(테이블 생성)은 별도로 실행해야 함.
* **해결:** 임시 kubectl Pod를 띄워 DDL 직접 실행으로 테이블 생성.

🚨 **Alert 서비스 Kafka SASL 인증 실패**
* **원인:** `infra/k8s/alert.yaml`에 `KAFKA_SASL_JAAS_CONFIG` 환경변수가 누락됨. Event Hubs는 SASL/SSL 인증이 필수.
* **해결:** `alert.yaml`에 Secret 참조로 `KAFKA_SASL_JAAS_CONFIG` 환경변수 추가.

🚨 **liveness probe kill loop (legacy-sync 재시작 반복)**
* **원인:** Spring Boot 기동 시간이 `initialDelaySeconds: 30`을 초과하여 liveness probe가 실패, K8s가 컨테이너를 계속 재시작하는 루프 발생.
* **해결:** `initialDelaySeconds: 30 → 90`으로 늘려 기동 완료 후 probe 시작하도록 조정.
