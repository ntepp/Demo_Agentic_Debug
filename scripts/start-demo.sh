#!/usr/bin/env bash
set -euo pipefail

JAVA_HOME_OVERRIDE="D:/app/jdk-25.0.3"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$SCRIPT_DIR/.."

echo "════════════════════════════════════════════════════════════"
echo "  Financial Demo — Agentic Engineering"
echo "  JDK: $JAVA_HOME_OVERRIDE"
echo "════════════════════════════════════════════════════════════"

export JAVA_HOME="$JAVA_HOME_OVERRIDE"
export PATH="$JAVA_HOME/bin:$PATH"
java -version

echo ""
echo "▶ Building order-service..."
cd "$ROOT/order-service"
./mvnw clean package -DskipTests -q
echo "✓ Build complete"

echo ""
echo "▶ Starting Docker stack (PostgreSQL + Loki + Grafana + Promtail)..."
cd "$ROOT/docker"
docker compose up -d
echo "✓ Stack starting..."

echo ""
echo "▶ Waiting for services (30s)..."
sleep 30

echo ""
echo "Services:"
curl -s http://localhost:8080/actuator/health | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'  order-service: {d[\"status\"]}')" 2>/dev/null || echo "  order-service: STARTING"
echo "  Grafana:       http://localhost:3000"
echo "  Loki query:    {job=\"financial-order-service\"} |= \"NullPointerException\""
echo "  App API:       http://localhost:8080/api/orders"
echo ""
echo "════════════════════════════════════════════════════════════"
echo "  Demo ready! NPE appears in logs within 10s."
echo "  Run: /debug TTP-2847"
echo "════════════════════════════════════════════════════════════"
