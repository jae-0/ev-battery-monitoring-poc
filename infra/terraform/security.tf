# KMS Key (SECURITY.md: RDS, DynamoDB at-rest 암호화)
resource "aws_kms_key" "main" {
  description             = "${var.project_name} KMS key for at-rest encryption"
  deletion_window_in_days = 7
  enable_key_rotation     = true
}

resource "aws_kms_alias" "main" {
  name          = "alias/${var.project_name}-key"
  target_key_id = aws_kms_key.main.key_id
}

# Secrets Manager: IoT API Key (SECURITY.md PoC Scope)
resource "aws_secretsmanager_secret" "iot_api_key" {
  name                    = "${var.project_name}/iot-api-key"
  description             = "EV IoT 단말기 API Key (PoC: X.509으로 전환 예정, TD-02)"
  kms_key_id              = aws_kms_key.main.arn
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "iot_api_key" {
  secret_id     = aws_secretsmanager_secret.iot_api_key.id
  secret_string = jsonencode({
    api_key = "REPLACE_WITH_ACTUAL_KEY"
  })

  lifecycle { ignore_changes = [secret_string] }
}
