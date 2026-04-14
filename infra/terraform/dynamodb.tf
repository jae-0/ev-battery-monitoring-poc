# DynamoDB: 배터리 텔레메트리 실시간 로그 저장 (docs/generated/db-schema.md 기준)
resource "aws_dynamodb_table" "battery_telemetry" {
  name         = "${var.project_name}-battery-telemetry"
  billing_mode = "PAY_PER_REQUEST"  # PoC: 트래픽 예측 불가, 온디맨드 사용
  hash_key     = "battery_id"
  range_key    = "timestamp"

  attribute {
    name = "battery_id"
    type = "S"
  }

  attribute {
    name = "timestamp"
    type = "N"
  }

  # SECURITY.md: at-rest 암호화 (KMS)
  server_side_encryption {
    enabled     = true
    kms_key_arn = aws_kms_key.main.arn
  }

  # RELIABILITY.md: Point-in-time recovery
  point_in_time_recovery { enabled = true }

  ttl {
    attribute_name = "ttl"
    enabled        = true
  }

  tags = { Name = "${var.project_name}-battery-telemetry" }
}
