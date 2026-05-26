package com.demo.financial.service;

import com.demo.financial.domain.FinancialOrder;
import com.demo.financial.domain.OrderStatus;
import com.demo.financial.repository.FinancialOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates batch reporting across all portfolios.
 * Groups PENDING orders by portfolio and processes each through the calculator.
 */
@Service
public class ReportingEngine {

    private static final Logger log = LoggerFactory.getLogger(ReportingEngine.class);

    private final FinancialAmountCalculator calculator;
    private final FinancialOrderRepository  orderRepository;

    public ReportingEngine(FinancialAmountCalculator calculator,
                           FinancialOrderRepository orderRepository) {
        this.calculator      = calculator;
        this.orderRepository = orderRepository;
    }

    /**
     * Generates a batch report for all PENDING orders.
     * A failure on one order does NOT abort the rest of the batch.
     */
    @Transactional
    public BatchReport generateBatchReport() {
        List<FinancialOrder> orders = orderRepository.findByStatus(OrderStatus.PENDING);
        log.info("Starting batch report — {} pending orders found", orders.size());

        if (orders.isEmpty()) {
            log.info("No pending orders — batch report skipped");
            return new BatchReport(0, List.of(), BigDecimal.ZERO);
        }

        Map<String, List<FinancialOrder>> byPortfolio = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getPortfolio().getId()));

        List<String> failed  = new ArrayList<>();
        BigDecimal   total   = BigDecimal.ZERO;
        int          success = 0;

        for (Map.Entry<String, List<FinancialOrder>> entry : byPortfolio.entrySet()) {
            String              portfolioId     = entry.getKey();
            List<FinancialOrder> portfolioOrders = entry.getValue();
            log.info("Processing {} orders for portfolio {}", portfolioOrders.size(), portfolioId);

            for (FinancialOrder order : portfolioOrders) {
                try {
                    BigDecimal calculated = calculator.calculateAmount(order);
                    total = total.add(calculated);
                    order.setStatus(OrderStatus.REPORTED);
                    orderRepository.save(order);
                    success++;
                    log.debug("Order {} processed successfully — amount={}", order.getOrderId(), calculated);
                } catch (NullPointerException e) {
                    log.error("Batch reporting failed for order={} portfolio={} — Currency is null",
                            order.getOrderId(), portfolioId);
                    log.error("java.lang.NullPointerException: Currency is null\n" +
                              "\tat com.demo.financial.service.FinancialAmountCalculator.calculateAmount(FinancialAmountCalculator.java:142)\n" +
                              "\tat com.demo.financial.service.ReportingEngine.generateBatchReport(ReportingEngine.java:67)\n" +
                              "\tat com.demo.financial.service.BatchReportingService.runBatchReport(BatchReportingService.java:41)");
                    order.setStatus(OrderStatus.FAILED);
                    orderRepository.save(order);
                    failed.add(order.getOrderId());
                } catch (Exception e) {
                    log.error("Unexpected error processing order={}: {}", order.getOrderId(), e.getMessage(), e);
                    order.setStatus(OrderStatus.FAILED);
                    orderRepository.save(order);
                    failed.add(order.getOrderId());
                }
            }
        }

        log.info("Batch report complete — success={} failed={} totalEUR={}",
                success, failed.size(), total);
        if (!failed.isEmpty()) {
            log.error("Batch cycle ended with {} FAILED orders: {}", failed.size(), failed);
        }

        return new BatchReport(success, failed, total);
    }

    public record BatchReport(int successCount, List<String> failedOrderIds, BigDecimal totalEur) {}
}
