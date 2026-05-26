package com.demo.financial.service;

import com.demo.financial.domain.FinancialOrder;
import com.demo.financial.domain.Portfolio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes portfolio-level risk metrics appended to each batch report.
 *
 * <p>Metrics computed:
 * <ul>
 *   <li>Concentration ratio — inverse of portfolio allocation weight (HHI contribution)</li>
 *   <li>Exposure index — total EUR exposure normalized by concentration</li>
 *   <li>Trader diversification — number of distinct traders per portfolio</li>
 * </ul>
 *
 * <p>These metrics are informational — they do not affect the reported amounts.
 * They are logged at INFO level and included in the batch summary.
 */
@Service
public class RiskCalculatorService {

    private static final Logger log = LoggerFactory.getLogger(RiskCalculatorService.class);

    private static final int METRIC_SCALE = 6;

    /**
     * Computes risk metrics for all portfolios represented in the given orders.
     *
     * @param orders the batch orders (may be from multiple portfolios)
     * @return a summary string for batch logging
     */
    public String computeBatchRiskMetrics(List<FinancialOrder> orders) {
        log.debug("Computing risk metrics for {} orders", orders.size());

        Map<String, List<FinancialOrder>> byPortfolio = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getPortfolio().getId()));

        StringBuilder summary = new StringBuilder();

        for (Map.Entry<String, List<FinancialOrder>> entry : byPortfolio.entrySet()) {
            String portfolioId = entry.getKey();
            List<FinancialOrder> portfolioOrders = entry.getValue();
            Portfolio portfolio = portfolioOrders.get(0).getPortfolio();

            log.debug("Computing risk metrics for portfolio={}", portfolioId);

            BigDecimal totalExposure = portfolioOrders.stream()
                    .map(FinancialOrder::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Concentration ratio: inverse of allocation weight
            // Measures how concentrated this portfolio is relative to the total allocation.
            // Higher value = more concentrated = higher risk.
            //
            // KAN-2: always specify scale + RoundingMode on divide() — weights like 0.35 or 0.45
            // produce non-terminating decimal quotients (1÷0.35 = 2.857142…) that BigDecimal
            // cannot represent exactly without a scale, causing ArithmeticException at runtime.
            BigDecimal weight = portfolio.getAllocationWeight() != null
                    ? portfolio.getAllocationWeight() : BigDecimal.ONE;
            BigDecimal concentrationRatio = BigDecimal.ONE.divide(weight, METRIC_SCALE, RoundingMode.HALF_UP);

            long traderCount = portfolioOrders.stream()
                    .map(FinancialOrder::getTraderId)
                    .distinct()
                    .count();

            BigDecimal exposureIndex = totalExposure.multiply(concentrationRatio)
                    .setScale(METRIC_SCALE, RoundingMode.HALF_UP);

            log.info("Risk metrics portfolio={} concentration={} exposure={} traders={}",
                    portfolioId, concentrationRatio, exposureIndex, traderCount);

            // Use Locale.US to ensure consistent decimal separators (.) regardless of JVM locale.
            // Financial metric output must be locale-independent.
            summary.append(String.format(Locale.US, "[%s] concentration=%.4f exposure=%.2f traders=%d | ",
                    portfolioId, concentrationRatio, exposureIndex, traderCount));
        }

        return summary.toString();
    }
}
