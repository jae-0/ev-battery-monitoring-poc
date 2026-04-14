# Database Schema Design

## 1. RDS PostgreSQL (Metadata)
- `vehicles`: vehicle_id(PK), driver_name, company_id
- `batteries`: battery_id(PK), vehicle_id(FK), status, updated_at

## 2. DynamoDB (Telemetry)
- PK: battery_id (String)
- SK: timestamp (Number)
- Attr: temperature, voltage, gps_lat, gps_lng