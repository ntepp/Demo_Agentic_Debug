package com.demo.financial.controller;

import com.demo.financial.domain.OrderStatus;
import com.demo.financial.dto.FinancialOrderDTO;
import com.demo.financial.repository.FinancialOrderRepository;
import com.demo.financial.service.ReportingEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final FinancialOrderRepository orderRepository;
    private final ReportingEngine          reportingEngine;

    public OrderController(FinancialOrderRepository orderRepository,
                           ReportingEngine reportingEngine) {
        this.orderRepository = orderRepository;
        this.reportingEngine = reportingEngine;
    }

    @GetMapping
    public List<FinancialOrderDTO> listOrders() {
        return orderRepository.findAll().stream()
                .map(FinancialOrderDTO::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinancialOrderDTO> getOrder(@PathVariable String id) {
        return orderRepository.findById(id)
                .map(FinancialOrderDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/failed")
    public List<FinancialOrderDTO> failedOrders() {
        return orderRepository.findByStatus(OrderStatus.FAILED).stream()
                .map(FinancialOrderDTO::from)
                .toList();
    }

    @GetMapping("/unconfigured")
    public List<FinancialOrderDTO> unconfiguredCurrencyOrders() {
        return orderRepository.findOrdersWithUnconfiguredPortfolioCurrency().stream()
                .map(FinancialOrderDTO::from)
                .toList();
    }

    @PostMapping("/batch-report")
    public ResponseEntity<ReportingEngine.BatchReport> triggerBatch() {
        ReportingEngine.BatchReport report = reportingEngine.generateBatchReport();
        return ResponseEntity.ok(report);
    }
}
