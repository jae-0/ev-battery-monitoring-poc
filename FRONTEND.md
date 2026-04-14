# Frontend: PoC Visualization Dashboard

본 PoC는 백엔드 및 인프라 검증이 주 목적이므로, 복잡한 UI 대신 관리형 도구를 활용합니다.

## 1. System Monitoring
- **도구:** Grafana (AWS Managed Prometheus 연동)
- **항목:** TPS, Latency, CPU/Memory Usage, Auto-scaling 현황.

## 2. Business Visualization
- **도구:** AWS QuickSight (Athena 기반) 또는 Grafana Business Dashboard.
- **항목:** 실시간 배터리 위치 지도, 위험 경고(Alert) 로그 시각화.