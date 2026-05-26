#!/usr/bin/env bash
# Reset all orders to PENDING so the bug re-triggers on the next batch cycle.
echo "▶ Resetting order statuses to PENDING..."
docker exec financial-postgres psql -U finuser -d financialdb \
  -c "UPDATE financial_order SET status = 'PENDING';"
echo "✓ Orders reset — next batch cycle will re-trigger TTP-2847."
