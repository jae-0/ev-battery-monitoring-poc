# outputs.tf
# @System-Architect-Agent: 이 파일의 값을 기반으로 contracts/infra-outputs.yaml을 작성합니다.
# terraform output -json 명령으로 실제 값을 확인 후 contracts 파일을 업데이트하세요.

output "eks_cluster_name" {
  description = "EKS 클러스터명 → contracts/infra-outputs.yaml: eks.cluster_name"
  value       = aws_eks_cluster.main.name
}

output "eks_region" {
  description = "EKS 리전 → contracts/infra-outputs.yaml: eks.region"
  value       = var.aws_region
}

output "msk_bootstrap_servers" {
  description = "MSK 브로커 주소 → contracts/infra-outputs.yaml: msk.bootstrap_servers"
  value       = aws_msk_cluster.main.bootstrap_brokers_tls
  sensitive   = true
}

output "kafka_topic_telemetry_raw" {
  description = "→ contracts/infra-outputs.yaml: msk.topics.telemetry_raw"
  value       = local.kafka_topics.telemetry_raw
}

output "kafka_topic_telemetry_processed" {
  value = local.kafka_topics.telemetry_processed
}

output "kafka_topic_alert_events" {
  value = local.kafka_topics.alert_events
}

output "kafka_topic_legacy_sync_queue" {
  value = local.kafka_topics.legacy_sync_queue
}

output "kafka_topic_dead_letter" {
  value = local.kafka_topics.dead_letter
}

output "rds_endpoint" {
  description = "RDS 엔드포인트 → contracts/infra-outputs.yaml: rds.endpoint"
  value       = aws_db_instance.main.address
  sensitive   = true
}

output "rds_secret_arn" {
  description = "RDS 자격증명 Secrets Manager ARN → contracts/infra-outputs.yaml: rds.secret_arn"
  value       = aws_db_instance.main.master_user_secret[0].secret_arn
  sensitive   = true
}

output "dynamodb_table_name" {
  description = "DynamoDB 테이블명 → contracts/infra-outputs.yaml: dynamodb.table_name"
  value       = aws_dynamodb_table.battery_telemetry.name
}

output "s3_datalake_bucket" {
  description = "S3 Data Lake 버킷명 → contracts/infra-outputs.yaml: s3.bucket_name"
  value       = aws_s3_bucket.datalake.bucket
}

output "kms_key_arn" {
  description = "KMS Key ARN"
  value       = aws_kms_key.main.arn
  sensitive   = true
}

output "iot_api_key_secret_arn" {
  description = "IoT API Key Secrets Manager ARN"
  value       = aws_secretsmanager_secret.iot_api_key.arn
}
