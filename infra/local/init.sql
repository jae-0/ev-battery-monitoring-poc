-- docs/generated/db-schema.md 기준 초기 스키마
-- Docker Compose 실행 시 postgres 컨테이너가 자동으로 실행

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id  VARCHAR(50) PRIMARY KEY,
    driver_name VARCHAR(100),
    company_id  VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS batteries (
    battery_id VARCHAR(50) PRIMARY KEY,
    vehicle_id VARCHAR(50) REFERENCES vehicles(vehicle_id),
    status     VARCHAR(20) NOT NULL DEFAULT 'IN_TRANSIT',
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- 로컬 테스트용 시드 데이터
INSERT INTO vehicles (vehicle_id, driver_name, company_id) VALUES
    ('VH-1001', '홍길동', 'GLOVIS-KR'),
    ('VH-1002', '김철수', 'GLOVIS-KR')
ON CONFLICT DO NOTHING;

INSERT INTO batteries (battery_id, vehicle_id, status) VALUES
    ('BAT-A001', 'VH-1001', 'IN_TRANSIT'),
    ('BAT-A002', 'VH-1001', 'IN_TRANSIT'),
    ('BAT-B001', 'VH-1002', 'IN_TRANSIT')
ON CONFLICT DO NOTHING;
