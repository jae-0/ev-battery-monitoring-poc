# EV Battery Real-time Monitoring System — Cloud Migration PoC

온프레미스로 운영 중인 EV 폐배터리 실시간 관제 시스템을 AWS 클라우드 기반 MSA로 전환하는 PoC 프로젝트입니다.  
인프라 설계부터 컨테이너 오케스트레이션, CI/CD, 부하 테스트까지 전 과정을 직접 구축했습니다.

## 성능 목표

| 지표 | 목표 |
|------|------|
| 처리량 | 10,000 TPS |
| p95 응답시간 | 500ms 미만 |
| 에러율 | 0.1% 미만 |

---

## 인프라 아키텍처

```
                        ┌─────────────────────────────────────────────┐
                        │                   AWS Cloud                  │
                        │                                              │
  IoT 단말기             │  ┌──────────┐      ┌─────────────────────┐  │
  (배터리 센서) ─────────┼─▶│   API    │─────▶│   EKS Cluster       │  │
                        │  │ Gateway  │      │                     │  │
                        │  │  + WAF   │      │  ┌───────────────┐  │  │
                        │  └──────────┘      │  │Telemetry Svc  │  │  │
                        │                    │  └──────┬────────┘  │  │
                        │  ┌──────────────┐  │         │ Kafka     │  │
                        │  │   Amazon     │◀─┼─────────┤           │  │
                        │  │   MSK        │  │         ▼           │  │
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

## 기술 스택

| 영역 | 기술 |
|------|------|
| IaC | Terraform |
| Container | Docker, Kubernetes (EKS) |
| CI/CD | GitHub Actions |
| Message Broker | Amazon MSK (Kafka) |
| Real-time DB | Amazon DynamoDB |
| Relational DB | Amazon RDS PostgreSQL 15 (Multi-AZ) |
| Security | AWS WAF, Secrets Manager, KMS |
| Monitoring | Prometheus, Grafana, CloudWatch |
| Load Test | JMeter (Docker CLI) |
| Application | Java 17 / Spring Boot 3.2 |

---

## 프로젝트 구조

```
.
├── infra/
│   ├── terraform/        # AWS 인프라 전체 IaC
│   │   ├── vpc.tf        # VPC, 서브넷, 라우팅
│   │   ├── eks.tf        # EKS 클러스터 + 노드그룹
│   │   ├── msk.tf        # Amazon MSK (Kafka)
│   │   ├── dynamodb.tf   # DynamoDB 테이블
│   │   ├── rds.tf        # RDS PostgreSQL Multi-AZ
│   │   ├── security.tf   # Security Group, WAF, KMS
│   │   ├── s3.tf         # Terraform 상태 원격 저장
│   │   └── variables.tf
│   └── k8s/              # Kubernetes 매니페스트
│       ├── namespace.yaml
│       ├── configmap.yaml
│       ├── telemetry.yaml    # Deployment + Service + HPA
│       ├── alert.yaml
│       └── legacy-sync.yaml
├── services/             # 마이크로서비스 소스코드
│   ├── telemetry/        # IoT 데이터 수집 (port 8080)
│   ├── alert/            # 이상 징후 탐지 (port 8081)
│   └── legacy-sync/      # 레거시 시스템 동기화 (port 8082)
├── cicd/
│   └── load-test/        # JMeter 시나리오
├── load-test/            # 로컬 부하 테스트
├── contracts/            # 서비스 간 API/Kafka 스키마 명세
└── docker-compose.yml    # 로컬 개발 환경 (인프라 대체재 포함)
```

---

## 인프라 설계 상세

### 네트워크 설계 (VPC)

- **Public Subnet**: API Gateway, NAT Gateway만 배치
- **Private Subnet**: EKS 워커 노드, RDS, MSK 전용
- 외부 → 앱 직접 접근 차단, API Gateway 경유 강제
- AWS WAF로 L7 레벨 공격 차단 (SQL Injection, XSS)

### Kubernetes (EKS)

- Multi-AZ 클러스터로 단일 AZ 장애 대응
- **HPA(Horizontal Pod Autoscaler)**: CPU 70% 초과 시 자동 스케일 아웃
- ConfigMap으로 환경별 설정 분리, Secret으로 민감값 관리
- 서비스별 NodePort → 내부 통신 / Ingress → 외부 노출

```bash
# 오토스케일링 예시
kubectl scale deployment telemetry-service -n battery-poc --replicas=3
kubectl get hpa -n battery-poc
```

### 메시지 브로커 (Amazon MSK)

서비스 간 완전한 디커플링을 위해 Kafka 사용.

| 토픽 | 파티션 | 용도 |
|------|--------|------|
| `telemetry-raw` | 3 | IoT 센서 데이터 원본 |
| `telemetry-processed` | 3 | 가공 데이터 |
| `alert-events` | 3 | 이상 징후 알림 |
| `legacy-sync-queue` | 1 | 레거시 동기화 큐 |
| `dead-letter-topic` | 1 | 처리 실패 메시지 격리 |

### 가용성 설계

- RDS Multi-AZ: 마스터 장애 시 자동 페일오버
- Circuit Breaker (Resilience4j): 하위 서비스 장애 전파 차단
- Dead Letter Topic: 3회 처리 실패 메시지 격리 후 재처리
- Idempotency: `batteryId + timestamp` 키로 중복 수신 무시

### 보안 설계

- **Secrets Manager**: DB 자격증명, IoT API Key 저장. 코드에 하드코딩 없음
- **KMS**: RDS, DynamoDB at-rest 암호화
- **TLS 1.2+**: 모든 서비스 간 통신 암호화 강제
- **최소 권한 원칙**: 서비스별 IAM Role 분리

---

## 로컬 개발 환경 (Docker Compose)

AWS 서비스를 로컬 대체재로 구성하여 클라우드 비용 없이 전체 플로우 검증.

| AWS 서비스 | 로컬 대체재 |
|-----------|------------|
| Amazon MSK | Confluent Kafka + ZooKeeper |
| DynamoDB | DynamoDB Local (인메모리) |
| RDS PostgreSQL | PostgreSQL 15 컨테이너 |

```bash
# 전체 인프라 + 서비스 실행 (의존성 순서 자동 관리)
docker compose up -d

# 데이터 플로우 검증
curl -X POST http://localhost:8080/v1/telemetry \
  -H "Content-Type: application/json" \
  -H "X-API-Key: local-dev-api-key" \
  -d '{
    "batteryId": "BAT-001",
    "vehicleId": "VEH-001",
    "timestamp": 1700000000000,
    "temperature": 65.0,
    "voltage": 400.0,
    "gpsLat": 37.5665,
    "gpsLng": 126.9780
  }'

# Alert 서비스에서 TEMPERATURE_EXCEEDED CRITICAL 탐지 확인
docker compose logs alert-service -f
```

---

## CI/CD 파이프라인 (GitHub Actions)

```
Push / PR
   │
   ├─ Build & Test ──── Maven 빌드 + JUnit 테스트 + JaCoCo 커버리지 80% 게이트
   │
   ├─ Docker Build ──── 멀티스테이지 빌드로 이미지 경량화
   │
   ├─ Push to ECR ───── AWS ECR 이미지 레지스트리 푸시
   │
   └─ Deploy to EKS ─── kubectl apply / Rolling Update 배포
```

- 커버리지 80% 미만 시 파이프라인 자동 실패
- Terraform plan 결과를 PR 코멘트로 자동 첨부

---

## Kubernetes 로컬 배포 (Docker Desktop)

```bash
# 1. 로컬 이미지 레지스트리 구동
docker run -d -p 5000:5000 registry:2

# 2. 이미지 빌드 & 푸시
docker build -t localhost:5000/battery-poc/telemetry-service services/telemetry
docker push localhost:5000/battery-poc/telemetry-service

docker build -t localhost:5000/battery-poc/alert-service services/alert
docker push localhost:5000/battery-poc/alert-service

docker build -t localhost:5000/battery-poc/legacy-sync-service services/legacy-sync
docker push localhost:5000/battery-poc/legacy-sync-service

# 3. 배포
kubectl apply -f infra/k8s/

# 4. 상태 확인
kubectl get pods,svc,hpa -n battery-poc
```

---

## AWS 배포 (Terraform)

```bash
cd infra/terraform

terraform init
terraform plan -var-file="terraform.tfvars"
terraform apply -var-file="terraform.tfvars"
```

`terraform.tfvars.example`을 복사해 실제 값을 채운 후 사용.

---

## 부하 테스트 (JMeter + Docker)

별도 설치 없이 Docker로 JMeter를 실행하는 방식을 채택.

```bash
docker run --rm \
  -v "$(pwd)/load-test:/load-test" \
  -v "$(pwd)/load-test/results:/results" \
  --add-host=host.docker.internal:host-gateway \
  justb4/jmeter:latest \
  -n -t /load-test/telemetry-load-test.jmx \
  -l /results/summary.jtl -e -o /results/html
```

**로컬 환경 측정 결과** (JMeter + 서비스 + 인프라 동일 PC 조건):

| 지표 | 결과 |
|------|------|
| TPS | 155.9 |
| 에러율 | **0.00%** |
| p95 | 1,417ms |

> p95가 기준(500ms)을 초과한 원인은 DynamoDB Local의 인메모리 I/O 시뮬레이션 지연과 단일 PC 리소스 경합. 실제 AWS DynamoDB는 단자리 ms 응답으로 기준 달성 가능.

---

## 단위 테스트

```bash
# 서비스별 테스트 실행
cd services/telemetry && docker run --rm -v "$(pwd):/app" -v "$HOME/.m2:/root/.m2" -w /app maven:3.9-eclipse-temurin-17 mvn test
cd services/alert && docker run --rm -v "$(pwd):/app" -v "$HOME/.m2:/root/.m2" -w /app maven:3.9-eclipse-temurin-17 mvn test
cd services/legacy-sync && docker run --rm -v "$(pwd):/app" -v "$HOME/.m2:/root/.m2" -w /app maven:3.9-eclipse-temurin-17 mvn test
```

| 서비스 | 테스트 수 | 커버리지 기준 |
|--------|-----------|--------------|
| Telemetry | 7개 | 80% 이상 |
| Alert | 10개 | 80% 이상 |
| Legacy Sync | 7개 | 80% 이상 |

---

## 트러블슈팅

### 1. ZooKeeper 헬스체크 실패

**증상**
```
zookeeper: dependency failed to start: container zookeeper is unhealthy
```

**원인**  
Confluent ZooKeeper 이미지에 `nc`(netcat) 명령어가 없어 헬스체크가 항상 실패.

**해결**  
`nc` 대신 Confluent 이미지에 내장된 `cub zk-ready` 명령어로 교체.

```yaml
healthcheck:
  test: ["CMD-SHELL", "cub zk-ready localhost:2181 10 2>/dev/null"]
```

---

### 2. DynamoDB Local 볼륨 퍼미션 오류

**증상**
```
java.nio.file.AccessDeniedException: /data/shared-local-instance.db
```

**원인**  
컨테이너 내부 유저가 마운트된 볼륨 경로(`/data`)에 쓰기 권한 없음.

**해결**  
볼륨 마운트 방식 대신 `-inMemory` 모드로 전환. 로컬 개발 환경에서는 재시작 시 데이터 초기화되므로 문제 없음.

```yaml
command: ["-jar", "DynamoDBLocal.jar", "-sharedDb", "-inMemory"]
```

---

### 3. `@Configuration` 내 `@PostConstruct` 실행 순서 문제

**증상**  
DynamoDB 테이블 생성 로직이 실행될 때 `NullPointerException` 발생.

**원인**  
`@Configuration` 클래스 안의 `@PostConstruct`는 `@Bean` 메서드 호출 전에 실행되어, 주입받으려는 `DynamoDbClient` 빈이 아직 초기화되지 않은 상태.

**해결**  
테이블 생성 로직을 별도 `@Component`로 분리하여 빈 초기화 이후 실행되도록 변경.

```java
@Component
@RequiredArgsConstructor
public class DynamoDbTableInitializer {
    private final DynamoDbClient dynamoDbClient;

    @PostConstruct
    public void createTableIfNotExists() { ... }
}
```

---

### 4. Kafka 메시지 역직렬화 실패 (ClassNotFoundException)

**증상**
```
ClassNotFoundException: com.battery.poc.telemetry.domain.TelemetryEvent
```

**원인**  
Kafka 메시지 헤더에 발행 서비스의 클래스 타입 정보(`__TypeId__`)가 포함되어 있고, 수신 서비스(Alert)가 이를 그대로 사용해 자신의 클래스패스에 없는 클래스를 로드하려 시도.

**해결**  
Alert 서비스 `application.yml`에 타입 헤더 무시 설정 추가.

```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.json.use.type.headers: false
        spring.json.value.default.type: com.battery.poc.alert.domain.TelemetryEvent
```

---

### 5. Spring Boot 3.2 `RestTemplateBuilder` API 제거

**증상**
```
cannot find symbol: method connectTimeout(Duration)
```

**원인**  
Spring Boot 3.2에서 `RestTemplateBuilder.connectTimeout(Duration)` 메서드 제거.

**해결**  
`SimpleClientHttpRequestFactory`로 직접 타임아웃 설정.

```java
SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
factory.setConnectTimeout(timeoutMs);
factory.setReadTimeout(timeoutMs);
return new RestTemplate(factory);
```

---

### 6. Kubernetes `ErrImageNeverPull`

**증상**
```
Failed to pull image "battery-poc/telemetry-service": ErrImageNeverPull
```

**원인**  
Docker Desktop의 containerd 이미지 스토어는 `docker build`로 빌드한 이미지를 K8s 파드가 직접 참조할 수 없음.

**해결**  
로컬 레지스트리(`localhost:5000`)를 띄우고 이미지를 push한 뒤 해당 주소로 배포.

```bash
docker run -d -p 5000:5000 registry:2
docker build -t localhost:5000/battery-poc/telemetry-service services/telemetry
docker push localhost:5000/battery-poc/telemetry-service
```

---

### 7. K8s 파드 → Kafka 연결 실패

**증상**
```
Connection refused: localhost:9092
```

**원인**  
Kafka가 `localhost:9092`로 advertise하면 K8s 파드 입장에서 `localhost`는 자기 자신(파드)을 가리켜 연결 불가.

**해결**  
`KAFKA_ADVERTISED_LISTENERS`를 `host.docker.internal`로 변경 후 Kafka 컨테이너 재생성.

```yaml
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://host.docker.internal:9092
```

> `docker compose restart kafka`는 환경변수를 반영하지 않으므로 반드시 `docker compose up -d kafka`로 재생성.

---

### 8. Mockito `Could not self-attach` (Java 17 + Docker)

**증상**
```
Could not self-attach to current VM using external process.
Mockito cannot mock this class
```

**원인**  
Java 17의 보안 정책으로 JVM self-attach가 기본 제한되며, Docker 컨테이너 환경에서 더욱 엄격하게 적용됨.

**해결**  
두 가지를 함께 적용.

1. `pom.xml` surefire 플러그인에 JVM 옵션 추가:
```xml
<argLine>-Djdk.attach.allowAttachSelf=true @{argLine}</argLine>
```

2. `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` 파일 생성:
```
mock-maker-subclass
```

---

### 9. `UnfinishedStubbingException` (Mockito)

**증상**
```
org.mockito.exceptions.misusing.UnfinishedStubbingException
```

**원인**  
`willReturn(List.of(battery(...)))` 안에서 `mock()` + `given()` 호출 시, Mockito 내부 stub 상태가 충돌.

**해결**  
mock 객체 생성과 stub 설정을 `given()` 호출 전에 분리.

```java
// 잘못된 방식
given(repository.findAll()).willReturn(List.of(battery("BAT-001", "IN_TRANSIT")));

// 올바른 방식
Battery bat = battery("BAT-001", "IN_TRANSIT");
given(repository.findAll()).willReturn(List.of(bat));
```
