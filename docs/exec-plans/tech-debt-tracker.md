# Tech Debt Tracker (기술 부채 기록)

| ID | 카테고리 | 내용 | 추후 해결 방안 |
|---|---|---|---|
| TD-01 | 인프라 | 단일 리전 구성 | Multi-Region Active-Active 구성 도입 |
| TD-02 | 보안 | API Key 기반 인증 | AWS IoT Core 기반 X.509 인증서 도입 |
| TD-03 | 연동 | 1시간 벌크 인서트 | CDC(Change Data Capture) 기반 실시간 동기화 도입 |