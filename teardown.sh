#!/bin/bash
# ============================================================
# teardown.sh — Azure 클라우드 리소스 전체 삭제 스크립트
# ============================================================
# ⚠️  LAWS.md DAT-05: 이 스크립트 실행 전 반드시 사람의 명시적 승인 필요
# ⚠️  실행 시 모든 Azure 리소스가 삭제되며 복구 불가능합니다
# ============================================================

set -e

RESOURCE_GROUP="glovis-poc-rg"
AKS_CLUSTER="glovis-poc-aks"
NAMESPACE="battery-poc"

echo "============================================"
echo "  EV 배터리 PoC — Azure 리소스 삭제 스크립트"
echo "============================================"
echo ""
echo "삭제 대상:"
echo "  - AKS 네임스페이스: $NAMESPACE"
echo "  - Terraform 관리 리소스 전체"
echo "  - Resource Group: $RESOURCE_GROUP"
echo ""
read -p "정말 삭제하시겠습니까? (yes 입력): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "취소됨."
  exit 0
fi

# ── Step 1: K8s 리소스 정리 ──────────────────────────────
echo ""
echo "[1/3] K8s 리소스 삭제 중..."

export PATH=$PATH:/home/yoonjaeyoung/.local/bin

if az aks get-credentials \
    --resource-group "$RESOURCE_GROUP" \
    --name "$AKS_CLUSTER" \
    --overwrite-existing 2>/dev/null; then
  kubectl delete namespace "$NAMESPACE" --ignore-not-found=true
  echo "  ✓ 네임스페이스 삭제 완료"
else
  echo "  ℹ AKS 접근 불가 — K8s 리소스 삭제 건너뜀"
fi

# ── Step 2: Terraform destroy ────────────────────────────
echo ""
echo "[2/3] Terraform 리소스 삭제 중... (10~20분 소요)"

cd "$(dirname "$0")/infra/terraform"

terraform destroy -auto-approve
echo "  ✓ Terraform 리소스 삭제 완료"

# ── Step 3: Resource Group 잔여 리소스 확인 ──────────────
echo ""
echo "[3/3] Resource Group 잔여 리소스 확인..."

REMAINING=$(az resource list --resource-group "$RESOURCE_GROUP" --query "length(@)" -o tsv 2>/dev/null || echo "0")

if [ "$REMAINING" -eq 0 ]; then
  echo "  ✓ 모든 리소스 삭제 완료"
  echo ""
  read -p "Resource Group '$RESOURCE_GROUP' 도 삭제하시겠습니까? (yes 입력): " CONFIRM_RG
  if [ "$CONFIRM_RG" = "yes" ]; then
    az group delete --name "$RESOURCE_GROUP" --yes --no-wait
    echo "  ✓ Resource Group 삭제 요청됨 (백그라운드 처리 중)"
  fi
else
  echo "  ⚠ 잔여 리소스 ${REMAINING}개 존재 — 수동 확인 필요"
  az resource list --resource-group "$RESOURCE_GROUP" --query "[].{name:name,type:type}" -o table
fi

echo ""
echo "============================================"
echo "  teardown 완료"
echo "============================================"
