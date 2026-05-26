package com.demo.financial.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a trading portfolio.
 *
 * <p>The {@link #currency} field is <b>nullable</b> for portfolios migrated from FinCore v1.
 * Those legacy portfolios store currency information at the order level only.
 */
@Entity
@Table(name = "portfolio")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    /** Geographic region for tax offset computation (e.g. "EMEA", "APAC", "AMER"). */
    private String region;

    /** Risk factor 0.0–1.0 applied during amount calculation. */
    private BigDecimal riskFactor;

    /** Weight of this portfolio within the batch report (0.0–1.0). */
    private BigDecimal allocationWeight;

    /** Regulatory type for capital buffer selection (e.g. "BASEL3", "MIFID2"). */
    private String regulationType;

    /**
     * Pre-calculated settlement currency.
     * <p><b>Nullable</b> — legacy FinCore v1 portfolios do not have this field populated.
     * The batch calculation will throw NPE when this is null (see TTP-2847).
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "code",   column = @Column(name = "currency_code")),
        @AttributeOverride(name = "amount", column = @Column(name = "currency_amount"))
    })
    private Currency currency;

    @OneToMany(mappedBy = "portfolio", fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<FinancialOrder> orders = new ArrayList<>();
}
