# Reliability: 시스템 가용성 및 장애 대응 계획 (BCP)

## 1. High Availability (고가용성 설계)
- **Multi-AZ (다중 가용 영역):** 핵심 서비스(API Gateway, EKS, RDS)는 최소 2개 이상의 가용 영역(AZ)에 분산 배치.
- **Auto-Scaling (자동 확장):** CPU 사용률 70% 초과 시 1분 이내에 추가 컨테이너를 자동으로 배포.

## 2. Load Testing Strategy (부하 테스트 전략)
- **테스트 시나리오:** Apache JMeter를 사용하여 10,000 TPS 도달 시 응답률 99.9%(에러율 0.1% 미만) 및 p95 Latency 500ms 미만 보장 테스트.
- **모니터링:** Prometheus와 Grafana를 통해 실시간 리소스 사용량 및 지연 시간 모니터링.

## 3. Fault Tolerance & Fallback (장애 내성)
- **Circuit Breaker Pattern:** 특정 마이크로서비스 장애 시 전체 전파 방지.
- **Dead Letter Topic (DLT):** 처리에 실패한 Kafka 메시지는 DLT로 전송하여 데이터 소실 방지.