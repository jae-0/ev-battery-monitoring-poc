resource "aws_security_group" "rds" {
  name   = "${var.project_name}-rds-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = var.private_subnet_cidrs
    description = "PostgreSQL (내부 VPC only)"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-rds-subnet-group"
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_db_instance" "main" {
  identifier        = "${var.project_name}-postgres"
  engine            = "postgres"
  engine_version    = "15.4"
  instance_class    = var.rds_instance_class
  allocated_storage = 20
  db_name           = var.rds_db_name
  username          = var.rds_username
  manage_master_user_password = true  # AWS Secrets Manager 자동 관리

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  # RELIABILITY.md: Multi-AZ
  multi_az = true

  # SECURITY.md: at-rest 암호화
  storage_encrypted = true
  kms_key_id        = aws_kms_key.main.arn

  # RELIABILITY.md: 자동 백업
  backup_retention_period = 7
  deletion_protection     = false  # PoC: 삭제 허용 (본 사업 시 true)
  skip_final_snapshot     = true   # PoC only

  tags = { Name = "${var.project_name}-postgres" }
}
