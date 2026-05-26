package com.demo.financial.dto;

import com.demo.financial.domain.FinancialOrder;
import com.demo.financial.domain.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for FinancialOrder — used for API input/output.
 */
@Data
@Builder
public class FinancialOrderDTO {

    private String orderId;
    private String portfolioId;
    private String portfolioName;
    private String currency;
    private BigDecimal amount;
    private String traderId;
    private OrderStatus status;
    private LocalDateTime createdAt;

    /** Whether the order's portfolio has a settlement currency configured. */
    private boolean currencyConfigured;

    public static FinancialOrderDTO from(FinancialOrder order) {
        return FinancialOrderDTO.builder()
                .orderId(order.getOrderId())
                .portfolioId(order.getPortfolio().getId())
                .portfolioName(order.getPortfolio().getName())
                .currency(order.getCurrency())
                .amount(order.getAmount())
                .traderId(order.getTraderId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .currencyConfigured(order.getPortfolio().getCurrency() != null)
                .build();
    }
}
