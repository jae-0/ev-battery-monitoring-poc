# Architecture Design: EV Battery Tracking System

## 1. Overall System Architecture (To-Be)
본 시스템은 AWS 클라우드 환경을 기반으로 하며, 마이크로서비스 아키텍처(MSA)를 채택하여 확장성과 독립성을 보장합니다.

- **Client Layer:** EV 운송 차량 내 IoT 단말기 (실시간 데이터 전송)
- **API Gateway:** 모든 외부 요청의 진입점 (인증 및 라우팅)
- **Message Broker (Amazon MSK - Managed Kafka):** 대규모 데이터 스트리밍의 버퍼 역할 (병목 방지)
- **Microservices (AWS EKS 기반 Docker 컨테이너):**
  - `Telemetry Service`: 센서 데이터 수집 및 가공
  - `Alert Service`: 이상 징후 탐지 및 알림 발송
  - `Legacy Sync Service`: 기존  온프레미스 시스템 연동
- **Data Layer:**
  - 시계열/NoSQL DB (Amazon DynamoDB): 고속의 실시간 로그 데이터 저장
  - RDBMS (Amazon RDS - PostgreSQL): 메타데이터 및 정형화된 배터리/차량 정보 저장
  - Data Lake (Amazon S3 + Athena): DynamoDB 데이터를 주기적으로 적재하여 비즈니스 분석(QuickSight) 용도로 활용

## 2. Infrastructure & DevOps
- **IaC (Infrastructure as Code):** Terraform을 활용한 인프라 프로비저닝.
- **CI/CD:** GitHub Actions를 활용하여 코드 커밋 시 Docker 이미지 자동 빌드 및 AWS EKS 환경에 무중단 배포.
- **Load Testing:** Apache JMeter를 활용하여 가상의 IoT 트래픽 부하 생성 및 시스템 병목 구간 식별.

## 3. Legacy Migration Strategy
- Strangler Fig Pattern을 적용하여, 기존 시스템의 기능을 점진적으로 클라우드 MSA로 이관하며 안정성 테스트 진행.