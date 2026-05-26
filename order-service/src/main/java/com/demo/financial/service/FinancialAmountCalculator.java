package com.demo.financial.service;

import com.demo.financial.domain.Currency;
import com.demo.financial.domain.FinancialOrder;
import com.demo.financial.domain.Portfolio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * Computes EUR-normalised, risk-adjusted, regulatory-buffered amounts
 * for FinancialOrders during batch reporting.
 *
 * <p>Calculation pipeline:
 * <ol>
 *   <li>Order validation</li>
 *   <li>Risk factor adjustment (portfolio-level)</li>
 *   <li>Regional tax offset</li>
 *   <li>Currency normalisation (order-level → EUR)</li>
 *   <li>Portfolio allocation weight</li>
 *   <li>Regulatory capital buffer</li>
 *   <li>Settlement currency multiplier (portfolio-level) ← NPE for legacy portfolios</li>
 * </ol>
 *
 * <p><b>Known issue — TTP-2847:</b> Step 7 reads
 * {@code portfolio.getCurrency().getAmount()} which throws
 * {@link NullPointerException} when the portfolio has no currency configured.
 * Legacy portfolios from FinCore v1 do not have this field populated.
 * Migration plan: TTP-2841.
 */
@Service
public class FinancialAmountCalculator {

    private static final Logger log =
            LoggerFactory.getLogger(FinancialAmountCalculator.class);

    private static final Map<String, BigDecimal> FX_RATES = Map.of(
            "EUR", BigDecimal.ONE,
            "USD", new BigDecimal("0.9218"),
            "GBP", new BigDecimal("1.1734"),
            "JPY", new BigDecimal("0.0062"),
            "CHF", new BigDecimal("1.0631")
    );

    private static final BigDecimal DEFAULT_RISK_FACTOR = new BigDecimal("0.05");
    private static final BigDecimal REGULATORY_BUFFER   = new BigDecimal("1.08");
    private static final int        REPORT_SCALE        = 4;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Calculates the EUR-normalised amount for a given order.
     *
     * @param order the financial order to process
     * @return EUR-normalised amount rounded to {@value #REPORT_SCALE} decimals
     */
    public BigDecimal calculateAmount(FinancialOrder order) {
        log.debug("Calculating amount for order={} trader={}",
                order.getOrderId(), order.getTraderId());

        validateOrder(order);

        Portfolio portfolio = order.getPortfolio();

        // Step 1 — risk-adjusted base amount
        BigDecimal risk = portfolio.getRiskFactor() != null
                ? portfolio.getRiskFactor() : DEFAULT_RISK_FACTOR;
        BigDecimal riskAdjusted = order.getAmount()
                .multiply(BigDecimal.ONE.subtract(risk))
                .setScale(REPORT_SCALE, RoundingMode.HALF_UP);
        log.debug("Risk-adjusted amount: {} (factor={})", riskAdjusted, risk);

        // Step 2 — regional tax offset
        BigDecimal taxOffset = computeTaxOffset(portfolio.getRegion(), riskAdjusted);
        BigDecimal afterTax  = riskAdjusted.subtract(taxOffset);
        log.debug("After-tax amount: {} (offset={})", afterTax, taxOffset);

        // Step 3 — order-level currency → EUR normalisation
        BigDecimal fxRate = FX_RATES.getOrDefault(order.getCurrency(), BigDecimal.ONE);
        BigDecimal eurNormalised = afterTax.multiply(fxRate)
                .setScale(REPORT_SCALE, RoundingMode.HALF_UP);
        log.debug("EUR-normalised: {} (rate={})", eurNormalised, fxRate);

        // Step 4 — portfolio allocation weight
        BigDecimal weight   = portfolio.getAllocationWeight() != null
                ? portfolio.getAllocationWeight() : BigDecimal.ONE;
        BigDecimal weighted = eurNormalised.multiply(weight)
                .setScale(REPORT_SCALE, RoundingMode.HALF_UP);
        log.debug("Weighted amount: {} (weight={})", weighted, weight);

        // Step 5 — regulatory capital buffer
        BigDecimal buffered = weighted.multiply(REGULATORY_BUFFER)
                .setScale(REPORT_SCALE, RoundingMode.HALF_UP);
        log.debug("After regulatory buffer: {}", buffered);

        // Step 6 — portfolio settlement currency multiplier
        //
        // Architecture (see Confluence — Financial Reporting Batch Architecture):
        //   The settlement engine populates Currency.amount during T+1 close.
        //   This value represents the EUR-equivalent exchange factor for the
        //   portfolio's primary settlement currency on the reporting date.
        //   It is stored at portfolio level so that all orders in the same
        //   portfolio share a single consistent conversion factor per batch run,
        //   avoiding per-order FX drift during high-volatility sessions.
        //
        // ⚠  BUG (TTP-2847): portfolio.getCurrency() is null for legacy
        //    FinCore v1 portfolios — no null-check was added when this step
        //    was introduced in commit a3f8e12 (Batch v2.1, 2024-07-15).
        //    NPE is thrown for every order whose portfolio lacks currency.
        //    Impact: entire batch cycle halts after the first failure.
        //    Affected portfolios: all FinCore v1 imports (e.g. EMERGING-MKT-C).
        //
        //    Correct fix — replace the assignment below with:
        //      Optional.ofNullable(portfolio.getCurrency())
        //              .map(Currency::getAmount)
        //              .orElse(BigDecimal.ONE)
        //
        //    This returns BigDecimal.ONE (neutral multiplier) when no settlement
        //    currency is configured, preserving the buffered amount as-is for
        //    legacy portfolios until TTP-2841 migration is complete.
        //
        //    See: FinancialAmountCalculatorTest#shouldHandleMissingCurrency()
        //
        // TODO(TTP-2847): add null safety — do not remove this comment until fixed
        //
        //    Reproduction: POST /api/orders/batch-report with portfolio EMERGING-MKT-C
        //    First observed: 2025-07-15 09:23:45 UTC (batch cycle #1)
        //    Recurrence: every batch cycle (~every 30s in demo mode)
        //    Severity: HIGH — no financial orders processed after this point
        //    Reporter: TRD-CHEN (discovered during end-of-day reconciliation)
        //    Environment: production (also reproducible in staging)
        //    Workaround: none — migration TTP-2841 must complete first
        //
        log.debug("Fetching settlement currency for portfolio id={}", portfolio.getId());
        // KAN-1: portfolio.getCurrency() is null for FinCore v1 legacy portfolios.
        // BigDecimal.ONE is the neutral multiplier — preserves the buffered amount
        // until TTP-2841 migration populates the currency field for all portfolios.
        BigDecimal settlementMultiplier = Optional.ofNullable(portfolio.getCurrency())
                .map(Currency::getAmount)
                .orElse(BigDecimal.ONE);

        BigDecimal result = buffered.multiply(settlementMultiplier)
                .setScale(REPORT_SCALE, RoundingMode.HALF_UP);
        log.info("Final calculated amount for order={}: {}", order.getOrderId(), result);
        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateOrder(FinancialOrder order) {
        if (order == null)
            throw new IllegalArgumentException("Order must not be null");
        if (order.getPortfolio() == null)
            throw new IllegalArgumentException("Portfolio must not be null");
        if (order.getAmount() == null)
            throw new IllegalArgumentException("Amount must not be null");
        if (order.getCurrency() == null)
            throw new IllegalArgumentException("Currency must not be null");
    }

    private BigDecimal computeTaxOffset(String region, BigDecimal amount) {
        BigDecimal taxRate = switch (region != null ? region : "DEFAULT") {
            case "EMEA" -> new BigDecimal("0.020");
            case "APAC" -> new BigDecimal("0.015");
            case "AMER" -> new BigDecimal("0.025");
            default     -> new BigDecimal("0.010");
        };
        return amount.multiply(taxRate).setScale(REPORT_SCALE, RoundingMode.HALF_UP);
    }
}
