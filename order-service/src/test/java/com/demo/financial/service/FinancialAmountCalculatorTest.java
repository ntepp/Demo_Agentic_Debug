package com.demo.financial.service;

import com.demo.financial.domain.FinancialOrder;
import com.demo.financial.domain.Portfolio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Regression tests for FinancialAmountCalculator.
 *
 * @see <a href="https://perinfinity.atlassian.net/browse/KAN-1">KAN-1</a>
 */
class FinancialAmountCalculatorTest {

    private final FinancialAmountCalculator calculator = new FinancialAmountCalculator();

    /**
     * Regression test for KAN-1 — NPE when portfolio.getCurrency() is null.
     *
     * <p>Legacy FinCore v1 portfolios (e.g. EMERGING-MKT-C) have no settlement
     * currency configured. The fix applies a neutral multiplier (BigDecimal.ONE)
     * so batch reporting continues without interruption.
     *
     * <p>Expected calculation (all-default portfolio, EUR order, amount=1000):
     * <ol>
     *   <li>Risk-adjusted:  1000.00 × (1 − 0.05)    = 950.0000</li>
     *   <li>After tax:      950.0000 − 9.5000        = 940.5000  (DEFAULT 1% region)</li>
     *   <li>EUR-normalised: 940.5000 × 1.0           = 940.5000  (EUR → EUR rate)</li>
     *   <li>Weighted:       940.5000 × 1.0           = 940.5000  (default weight)</li>
     *   <li>Buffered:       940.5000 × 1.08          = 1015.7400</li>
     *   <li>Settlement:     1015.7400 × 1.0          = 1015.7400 (neutral fallback)</li>
     * </ol>
     */
    @Test
    void shouldHandleMissingCurrency() {
        // Arrange — FinCore v1 legacy portfolio: no settlement currency configured
        Portfolio legacyPortfolio = Portfolio.builder()
                .id("EMERGING-MKT-C")
                .name("Emerging Markets C (FinCore v1)")
                .build(); // currency = null intentionally

        FinancialOrder order = FinancialOrder.builder()
                .orderId("ORD-008")
                .portfolio(legacyPortfolio)
                .currency("EUR")
                .amount(new BigDecimal("1000.00"))
                .traderId("TRD-CHEN")
                .build();

        // Act — must not throw NullPointerException
        BigDecimal result = assertDoesNotThrow(
                () -> calculator.calculateAmount(order),
                "calculateAmount() must not throw NPE for a portfolio with null currency (FinCore v1 legacy)"
        );

        // Assert — neutral multiplier (BigDecimal.ONE) applied; amount equals buffered value
        assertThat(result)
                .as("Expected neutral settlement multiplier (1.0) for legacy portfolio")
                .isEqualByComparingTo(new BigDecimal("1015.7400"));
    }
}
