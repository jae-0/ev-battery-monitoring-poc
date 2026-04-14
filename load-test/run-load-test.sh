#!/bin/bash
# JMeter 부하 테스트 실행 스크립트 (Docker CLI 방식)
# 사용법: bash load-test/run-load-test.sh [TPS목표]

TARGET_TPS=${1:-500}
THROUGHPUT=$(echo "$TARGET_TPS * 60" | bc)  # JMeter는 분당 요청 수로 설정

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/results"
mkdir -p "$RESULTS_DIR"

echo "======================================"
echo " EV Battery PoC - JMeter 부하 테스트 시작"
echo " 목표 TPS  : $TARGET_TPS"
echo " 분당 요청 : $THROUGHPUT"
echo " 결과 경로 : $RESULTS_DIR"
echo "======================================"

# 이전 결과 정리
rm -f "$RESULTS_DIR/summary.jtl"
rm -rf "$RESULTS_DIR/html"

docker run --rm \
  -v "$SCRIPT_DIR:/load-test" \
  -v "$RESULTS_DIR:/results" \
  --add-host=host.docker.internal:host-gateway \
  justb4/jmeter:latest \
  -n \
  -t /load-test/telemetry-load-test.jmx \
  -l /results/summary.jtl \
  -e \
  -o /results/html \
  -JTPS=$TARGET_TPS \
  -Jthroughput=$THROUGHPUT \
  2>&1

echo ""
echo "======================================"
echo " 테스트 완료. 결과 파싱 중..."
echo "======================================"

# JTL 파싱: 간단 통계 출력
if [ -f "$RESULTS_DIR/summary.jtl" ]; then
  python3 - <<'PYEOF'
import csv, sys, os

jtl = os.environ.get('RESULTS_DIR', '/results') + '/summary.jtl'
try:
    jtl = open('/results/summary.jtl') if os.path.exists('/results/summary.jtl') else open(os.path.join(os.path.dirname(__file__), 'results/summary.jtl'))
except:
    jtl = open(os.path.expanduser('~') + '/results/summary.jtl')

rows = []
with open(jtl.name) as f:
    reader = csv.DictReader(f)
    for row in reader:
        rows.append(row)

if not rows:
    print("결과 없음")
    sys.exit(1)

elapsed = [int(r['elapsed']) for r in rows]
success = [r for r in rows if r['success'] == 'true']
elapsed.sort()

total = len(rows)
errors = total - len(success)
error_rate = errors / total * 100
avg = sum(elapsed) / total
p95 = elapsed[int(total * 0.95)]
p99 = elapsed[int(total * 0.99)]
duration_sec = (int(rows[-1]['timeStamp']) - int(rows[0]['timeStamp'])) / 1000
tps = total / duration_sec if duration_sec > 0 else 0

print(f"\n{'='*40}")
print(f"  총 요청 수  : {total:,}")
print(f"  실제 TPS   : {tps:.1f}")
print(f"  에러율      : {error_rate:.2f}%")
print(f"  평균 응답   : {avg:.0f}ms")
print(f"  p95 응답    : {p95}ms")
print(f"  p99 응답    : {p99}ms")
print(f"{'='*40}")

# 성공 기준 체크 (QUALITY_SCORE.md: p95 < 500ms, 에러율 < 0.1%)
ok = True
if p95 >= 500:
    print(f"  ❌ p95 {p95}ms >= 500ms 기준 미달")
    ok = False
else:
    print(f"  ✅ p95 {p95}ms < 500ms 기준 충족")

if error_rate >= 0.1:
    print(f"  ❌ 에러율 {error_rate:.2f}% >= 0.1% 기준 미달")
    ok = False
else:
    print(f"  ✅ 에러율 {error_rate:.2f}% < 0.1% 기준 충족")

print(f"{'='*40}\n")
PYEOF
fi

echo "HTML 리포트: $RESULTS_DIR/html/index.html"
