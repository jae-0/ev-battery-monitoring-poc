# Design Principles: Software Architecture

1. **Stateless (상태 비저장):** 모든 마이크로서비스는 상태를 내부에 저장하지 않음.
2. **API First:** 모든 서비스 인터페이스는 구현 전에 Swagger/OpenAPI 명세를 먼저 정의한다. (외부 클라이언트 ↔ API Gateway 구간 적용)
3. **Decoupling (결합도 최소화):** 내부 마이크로서비스 간 통신은 반드시 Amazon MSK(Kafka)를 통한 비동기 방식 지향.
4. **Idempotency (멱등성):** 동일 데이터 중복 수신 시에도 데이터 무결성 보장 로직 구현.