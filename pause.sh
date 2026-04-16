#!/bin/bash
# ============================================================
# pause.sh — Azure 비용 절감 스크립트 (리소스 중지)
# 작업 안 할 때 실행 → 컴퓨팅 비용 차단
# 재개 시 resume.sh 실행
# ============================================================

RESOURCE_GROUP="glovis-poc-rg"
AKS_CLUSTER="glovis-poc-aks"
NODE_POOL="default"
POSTGRES_SERVER="glovis-poc-postgres"

echo "======================================"
echo "  PoC 환경 일시 중지 (비용 절감)"
echo "======================================"

# ── AKS 노드풀 0으로 축소 (VM 비용 차단) ──
echo ""
echo "[1/2] AKS 노드풀 중지 중..."
az aks nodepool scale \
  --resource-group "$RESOURCE_GROUP" \
  --cluster-name "$AKS_CLUSTER" \
  --name "$NODE_POOL" \
  --node-count 0 \
  --no-wait
echo "  ✓ AKS 노드풀 0으로 축소 요청 (백그라운드 처리 중)"

# ── PostgreSQL 중지 ──
echo ""
echo "[2/2] PostgreSQL 중지 중..."
az postgres flexible-server stop \
  --resource-group "$RESOURCE_GROUP" \
  --name "$POSTGRES_SERVER"
echo "  ✓ PostgreSQL 중지 완료"

echo ""
echo "======================================"
echo "  중지 완료"
echo "  재개하려면: ./resume.sh"
echo "======================================"
echo ""
echo "※ 계속 과금되는 리소스 (중지 불가):"
echo "  - Application Gateway WAF_v2: ~$0.35/시간"
echo "  - NAT Gateway: ~$0.045/시간"
echo "  - Public IP: 소액"
echo "  완전 차단하려면 teardown.sh 사용"
