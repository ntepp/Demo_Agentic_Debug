package com.demo.financial.service;

import com.demo.financial.domain.FinancialOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a trader-level audit summary after each batch cycle.
 *
 * <p>For each trader, computes:
 * <ul>
 *   <li>Number of orders submitted in the cycle</li>
 *   <li>Numeric trader code (extracted from trader ID) for legacy audit system</li>
 *   <li>Total exposure across all submitted orders</li>
 * </ul>
 *
 * <p><b>Known issue — KAN-3:</b> {@link #extractTraderCode} calls
 * {@link Integer#parseInt} on the raw traderId string (e.g. {@code "TRD-MARTIN"}).
 * The legacy audit system expected purely numeric IDs, but the current naming
 * convention uses the format {@code TRD-<NAME>}. This throws
 * {@link NumberFormatException} for every order in the batch.
 */
@Service
public class TraderAuditService {

    private static final Logger log = LoggerFactory.getLogger(TraderAuditService.class);

    /**
     * Produces a per-trader audit summary for the given order list.
     *
     * @param orders completed orders from the current batch cycle
     */
    public void generateAuditSummary(List<FinancialOrder> orders) {
        log.debug("Generating trader audit summary for {} orders", orders.size());

        Map<String, List<FinancialOrder>> byTrader = orders.stream()
                .collect(Collectors.groupingBy(FinancialOrder::getTraderId));

        for (Map.Entry<String, List<FinancialOrder>> entry : byTrader.entrySet()) {
            String traderId = entry.getKey();
            List<FinancialOrder> traderOrders = entry.getValue();

            // Extract numeric code for legacy audit system integration.
            // The audit system was built when trader IDs were purely numeric (e.g. "4821").
            // The new naming convention "TRD-<NAME>" was adopted in Q2 2024 but this
            // extraction logic was never updated.
            //
            // ⚠  BUG (KAN-3): Integer.parseInt(traderId) throws NumberFormatException
            //    for any traderId matching the current "TRD-<NAME>" format.
            //    Every trader in the system uses this format → every audit call fails.
            //
            //    Fix: parse only the numeric suffix, or use a dedicated mapping table.
            //      e.g. traderId.replaceAll("[^0-9]", "") — then parseInt if non-empty
            //
            // TODO(KAN-3): update extraction logic for new trader ID format
            int traderCode = extractTraderCode(traderId); // ← NumberFormatException

            log.info("Audit — trader={} code={} orders={}", traderId, traderCode, traderOrders.size());
        }
    }

    /**
     * Extracts the numeric trader code from the trader ID.
     * Assumes the ID is purely numeric — valid for IDs created before Q2 2024.
     *
     * @param traderId the raw trader identifier
     * @return numeric trader code
     * @throws NumberFormatException if traderId is not a valid integer (e.g. "TRD-MARTIN")
     */
    private int extractTraderCode(String traderId) {
        return Integer.parseInt(traderId); // ← throws for "TRD-MARTIN", "TRD-CHEN", etc.
    }
}
