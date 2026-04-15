# Security: 보안 및 데이터 보호 전략

## 1. Network Security (네트워크 보안)
- **VPC 및 Subnet 격리:** API Gateway만 Public Subnet 배치, App/DB는 Private Subnet 배치.
- **Security Groups & WAF:** AWS WAF를 적용하여 악의적인 공격 방어.

## 2. Authentication & Authorization (인증 및 인가)
- **기기 인증 (PoC Scope):** EV 운송 차량의 IoT 단말기는 AWS Secrets Manager에 등록된 API Key를 통해 접근. (본 사업 시 X.509 인증서 도입 예정)
- **mTLS:** 내부 마이크로서비스 간 통신 시 Mutual TLS 적용 권장.

## 3. Data Encryption (데이터 암호화)
- **In-Transit:** 모든 통신은 HTTPS(TLS 1.2+) 적용.
- **At-Rest:** RDS 및 DynamoDB 데이터는 AWS KMS를 활용하여 디스크 레벨 암호화.