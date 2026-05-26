package com.demo.financial.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled service that triggers the batch reporting cycle.
 * In demo mode runs every 30s (initialDelay 10s) so the failure appears quickly.
 * In production this would run every 15 minutes.
 */
@Service
public class BatchReportingService {

    private static final Logger log = LoggerFactory.getLogger(BatchReportingService.class);

    private final ReportingEngine reportingEngine;
    private final AtomicInteger   cycleCounter = new AtomicInteger(0);

    public BatchReportingService(ReportingEngine reportingEngine) {
        this.reportingEngine = reportingEngine;
    }

    /**
     * Demo interval: every 30 seconds.
     * Production: {@code cron = "0 0/15 * * * *"}
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    public void runBatchReport() {
        int cycle = cycleCounter.incrementAndGet();
        log.info("══════════════════════════════════════════════════════════");
        log.info("Starting batch reporting cycle #{}", cycle);
        log.info("══════════════════════════════════════════════════════════");

        try {
            ReportingEngine.BatchReport report = reportingEngine.generateBatchReport();
            if (report.failedOrderIds().isEmpty()) {
                log.info("Cycle #{} — all {} orders reported successfully. Total EUR: {}",
                        cycle, report.successCount(), report.totalEur());
            } else {
                log.error("Cycle #{} — {} orders FAILED. See ReportingEngine logs for stack traces.",
                        cycle, report.failedOrderIds().size());
            }
        } catch (Exception e) {
            log.error("Cycle #{} — batch reporting terminated unexpectedly: {}", cycle, e.getMessage(), e);
        }
    }
}
