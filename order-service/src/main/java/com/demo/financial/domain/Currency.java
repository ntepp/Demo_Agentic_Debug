package com.demo.financial.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents the settlement currency configuration for a Portfolio.
 * Contains the pre-calculated settlement amount for the current reporting period.
 *
 * <p>This object is <b>nullable</b> for legacy portfolios imported from FinCore v1.
 * Those portfolios store currency information at the order level only.
 * Migration is tracked in TTP-2841.
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

    /** ISO 4217 currency code (e.g. "EUR", "USD"). */
    private String code;

    /**
     * Pre-calculated settlement amount for the reporting period.
     * Populated at market close by the settlement engine.
     */
    private BigDecimal amount;
}
