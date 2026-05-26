package com.demo.financial.service;

import com.demo.financial.domain.FinancialOrder;
import com.demo.financial.domain.Portfolio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Regression tests for RiskCalculatorService.
 *
 * @see <a href="https://perinfinity.atlassian.net/browse/KAN-2">KAN-2</a>
 */
class RiskCalculatorServiceTest {

    private final RiskCalculatorService service = new RiskCalculatorService();

    /**
     * Regression test for KAN-2 — ArithmeticException when allocationWeight produces a
     * non-terminating decimal quotient on division.
     *
     * <p>Affected portfolios: EQUITY-FUND-A (weight=0.35) and FIXED-INCOME-B (weight=0.45).
     * Both produce infinite decimal expansions when inverted:
     * <ul>
     *   <li>1 ÷ 0.35 = 2.857142857… → ArithmeticException without RoundingMode</li>
     *   <li>1 ÷ 0.45 = 2.222222222… → ArithmeticException without RoundingMode</li>
     * </ul>
     *
     * <p>The fix specifies {@code scale=6, RoundingMode.HALF_UP}, yielding:
     * <ul>
     *   <li>EQUITY-FUND-A  → concentration = 2.857143</li>
     *   <li>FIXED-INCOME-B → concentration = 2.222222</li>
     * </ul>
     */
    @Test
    void shouldComputeConcentrationRatioWithoutArithmeticException() {
        // Arrange — both portfolios that triggered the ArithmeticException in production
        Portfolio equityPortfolio = Portfolio.builder()
                .id("EQUITY-FUND-A")
                .name("Equity Fund A")
                .allocationWeight(new BigDecimal("0.35"))
                .build();

        Portfolio fixedIncomePortfolio = Portfolio.builder()
                .id("FIXED-INCOME-B")
                .name("Fixed Income B")
                .allocationWeight(new BigDecimal("0.45"))
                .build();

        List<FinancialOrder> orders = List.of(
                FinancialOrder.builder()
                        .orderId("ORD-100")
                        .portfolio(equityPortfolio)
                        .currency("EUR")
                        .amount(new BigDecimal("10000.00"))
                        .traderId("TRD-001")
                        .build(),
                FinancialOrder.builder()
                        .orderId("ORD-101")
                        .portfolio(fixedIncomePortfolio)
                        .currency("EUR")
                        .amount(new BigDecimal("20000.00"))
                        .traderId("TRD-002")
                        .build()
        );

        // Act — must not throw ArithmeticException: Non-terminating decimal expansion
        String summary = assertDoesNotThrow(
                () -> service.computeBatchRiskMetrics(orders),
                "computeBatchRiskMetrics() must not throw ArithmeticException for weights 0.35 and 0.45"
        );

        // Assert — both portfolios appear in the summary with correct rounded ratios
        assertThat(summary)
                .as("Summary must contain EQUITY-FUND-A metrics")
                .contains("EQUITY-FUND-A")
                .contains("concentration=2.8571");  // 1 ÷ 0.35, HALF_UP, 4 decimal places in format

        assertThat(summary)
                .as("Summary must contain FIXED-INCOME-B metrics")
                .contains("FIXED-INCOME-B")
                .contains("concentration=2.2222");  // 1 ÷ 0.45, HALF_UP, 4 decimal places in format
    }

    /**
     * Verifies that a portfolio with no allocationWeight configured (null → defaults to 1.0)
     * still computes correctly — concentration ratio = 1 ÷ 1.0 = 1.000000.
     */
    @Test
    void shouldHandleNullAllocationWeightWithDefaultOne() {
        Portfolio portfolio = Portfolio.builder()
                .id("DEFAULT-PORTFOLIO")
                .name("Default Portfolio")
                .build(); // allocationWeight = null → defaults to BigDecimal.ONE

        FinancialOrder order = FinancialOrder.builder()
                .orderId("ORD-200")
                .portfolio(portfolio)
                .currency("EUR")
                .amount(new BigDecimal("5000.00"))
                .traderId("TRD-003")
                .build();

        String summary = assertDoesNotThrow(
                () -> service.computeBatchRiskMetrics(List.of(order)),
                "computeBatchRiskMetrics() must handle null allocationWeight gracefully"
        );

        assertThat(summary)
                .contains("DEFAULT-PORTFOLIO")
                .contains("concentration=1.0000");  // 1 ÷ 1.0 = 1.000000
    }
}
