package com.demo.financial.config;

import com.demo.financial.domain.*;
import com.demo.financial.repository.FinancialOrderRepository;
import com.demo.financial.repository.PortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds the database with demo data on startup.
 * Creates portfolios (some with, some without currency) and sample orders.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedDemoData(PortfolioRepository portfolioRepo,
                                   FinancialOrderRepository orderRepo) {
        return args -> {
            if (portfolioRepo.count() > 0) {
                log.info("Demo data already present — skipping seed");
                return;
            }

            log.info("Seeding demo portfolios and orders...");

            // ── Portfolios WITH currency configured (modern — will succeed) ──
            Portfolio equityFund = portfolioRepo.save(Portfolio.builder()
                    .id("EQUITY-FUND-A")
                    .name("Global Equity Fund A")
                    .region("EMEA")
                    .riskFactor(new BigDecimal("0.04"))
                    .allocationWeight(new BigDecimal("0.35"))
                    .regulationType("MIFID2")
                    .currency(Currency.builder()
                            .code("EUR")
                            .amount(new BigDecimal("1.0000"))
                            .build())
                    .build());

            Portfolio fixedIncome = portfolioRepo.save(Portfolio.builder()
                    .id("FIXED-INCOME-B")
                    .name("Fixed Income Portfolio B")
                    .region("AMER")
                    .riskFactor(new BigDecimal("0.02"))
                    .allocationWeight(new BigDecimal("0.45"))
                    .regulationType("BASEL3")
                    .currency(Currency.builder()
                            .code("USD")
                            .amount(new BigDecimal("0.9218"))
                            .build())
                    .build());

            // ── Portfolio WITHOUT currency — legacy FinCore v1 — TRIGGERS NPE ──
            Portfolio emergingMarket = portfolioRepo.save(Portfolio.builder()
                    .id("EMERGING-MKT-C")
                    .name("Emerging Markets Portfolio C")
                    .region("APAC")
                    .riskFactor(new BigDecimal("0.08"))
                    .allocationWeight(new BigDecimal("0.20"))
                    .regulationType("BASEL3")
                    .currency(null)   // ← legacy FinCore v1: no currency configured
                    .build());

            log.warn("Portfolio EMERGING-MKT-C has no settlement currency (legacy FinCore v1). " +
                     "Batch reporting WILL fail for its orders — see TTP-2847.");

            // ── Orders for EQUITY-FUND-A (will succeed) ──────────────────────
            saveAll(orderRepo, List.of(
                    order("ORD-001", equityFund, "EUR", "125000.00", "TRD-MARTIN"),
                    order("ORD-002", equityFund, "USD", "98500.00",  "TRD-DUPONT"),
                    order("ORD-003", equityFund, "GBP", "212000.00", "TRD-MARTIN"),
                    order("ORD-004", equityFund, "EUR", "67800.00",  "TRD-WEBER")
            ));

            // ── Orders for FIXED-INCOME-B (will succeed) ─────────────────────
            saveAll(orderRepo, List.of(
                    order("ORD-005", fixedIncome, "USD", "450000.00", "TRD-CHEN"),
                    order("ORD-006", fixedIncome, "EUR", "320000.00", "TRD-DUPONT"),
                    order("ORD-007", fixedIncome, "CHF", "180000.00", "TRD-WEBER")
            ));

            // ── Orders for EMERGING-MKT-C (WILL FAIL with NPE) ───────────────
            saveAll(orderRepo, List.of(
                    order("ORD-008", emergingMarket, "USD", "750000.00",   "TRD-CHEN"),
                    order("ORD-009", emergingMarket, "JPY", "95000000.00", "TRD-TANAKA"),
                    order("ORD-010", emergingMarket, "EUR", "430000.00",   "TRD-MARTIN")
            ));

            log.info("Demo data seeded — 3 portfolios, 10 orders (ORD-008/009/010 will trigger TTP-2847)");
        };
    }

    private void saveAll(FinancialOrderRepository repo, List<FinancialOrder> orders) {
        orders.forEach(repo::save);
    }

    private FinancialOrder order(String id, Portfolio portfolio,
                                  String currency, String amount, String trader) {
        return FinancialOrder.builder()
                .orderId(id)
                .portfolio(portfolio)
                .currency(currency)
                .amount(new BigDecimal(amount))
                .traderId(trader)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
