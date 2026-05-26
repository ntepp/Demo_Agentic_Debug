package com.demo.financial.repository;

import com.demo.financial.domain.FinancialOrder;
import com.demo.financial.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialOrderRepository extends JpaRepository<FinancialOrder, String> {

    List<FinancialOrder> findByPortfolioId(String portfolioId);

    List<FinancialOrder> findByStatus(OrderStatus status);

    @Query("SELECT o FROM FinancialOrder o WHERE o.portfolio.currency IS NULL")
    List<FinancialOrder> findOrdersWithUnconfiguredPortfolioCurrency();
}
