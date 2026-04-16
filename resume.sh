#!/bin/bash
# ============================================================
# resume.sh — Azure 환경 재개 스크립트
# pause.sh 실행 후 다시 작업할 때 실행
# ============================================================

RESOURCE_GROUP="glovis-poc-rg"
AKS_CLUSTER="glovis-poc-aks"
NODE_POOL="default"
POSTGRES_SERVER="glovis-poc-postgres"

echo "======================================"
echo "  PoC 환경 재개"
echo "======================================"

# ── PostgreSQL 시작 ──
echo ""
echo "[1/3] PostgreSQL 시작 중..."
az postgres flexible-server start \
  --resource-group "$RESOURCE_GROUP" \
  --name "$POSTGRES_SERVER"
echo "  ✓ PostgreSQL 시작 완료"

# ── AKS 노드풀 복구 ──
echo ""
echo "[2/3] AKS 노드풀 복구 중..."
az aks nodepool scale \
  --resource-group "$RESOURCE_GROUP" \
  --cluster-name "$AKS_CLUSTER" \
  --name "$NODE_POOL" \
  --node-count 2
echo "  ✓ AKS 노드풀 2개로 복구 완료"

# ── kubeconfig 갱신 ──
echo ""
echo "[3/3] kubeconfig 갱신 중..."
az aks get-credentials \
  --resource-group "$RESOURCE_GROUP" \
  --name "$AKS_CLUSTER" \
  --overwrite-existing
echo "  ✓ kubeconfig 갱신 완료"

echo ""
echo "======================================"
echo "  재개 완료"
echo "  Pod 상태 확인:"
echo "  export PATH=\$PATH:/home/yoonjaeyoung/.local/bin"
echo "  kubectl get pods -n battery-poc"
echo "======================================"
