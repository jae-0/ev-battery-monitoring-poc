# S3 Data Lake + Athena (ARCHITECTURE.md: QuickSight 비즈니스 시각화용)
resource "aws_s3_bucket" "datalake" {
  bucket = "${var.project_name}-datalake"
  tags   = { Name = "${var.project_name}-datalake" }
}

resource "aws_s3_bucket_versioning" "datalake" {
  bucket = aws_s3_bucket.datalake.id
  versioning_configuration { status = "Enabled" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "datalake" {
  bucket = aws_s3_bucket.datalake.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.main.arn
    }
  }
}

resource "aws_s3_bucket_public_access_block" "datalake" {
  bucket                  = aws_s3_bucket.datalake.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Athena 쿼리 결과 저장 버킷
resource "aws_s3_bucket" "athena_results" {
  bucket = "${var.project_name}-athena-results"
  tags   = { Name = "${var.project_name}-athena-results" }
}

resource "aws_s3_bucket_public_access_block" "athena_results" {
  bucket                  = aws_s3_bucket.athena_results.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_athena_workgroup" "main" {
  name = "${var.project_name}-workgroup"

  configuration {
    result_configuration {
      output_location = "s3://${aws_s3_bucket.athena_results.bucket}/results/"
    }
  }
}

resource "aws_glue_catalog_database" "main" {
  name = "${var.project_name}_catalog"
}
