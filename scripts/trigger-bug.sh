#!/usr/bin/env bash
# Manually trigger one batch cycle to produce NPE logs immediately.
echo "▶ Triggering batch report (NPE expected for EMERGING-MKT-C orders)..."
curl -s -X POST http://localhost:8080/api/orders/batch-report | python3 -m json.tool
echo ""
echo "▶ Failed orders:"
curl -s http://localhost:8080/api/orders/failed | python3 -m json.tool
