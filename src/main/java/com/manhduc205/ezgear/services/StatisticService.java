package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.responses.*;
import com.manhduc205.ezgear.dtos.responses.reports.ChartDataResponse;
import com.manhduc205.ezgear.dtos.responses.reports.DashboardSummaryResponse;
import com.manhduc205.ezgear.dtos.responses.reports.ProductStatResponse;
import com.manhduc205.ezgear.repositories.StatisticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final StatisticRepository statRepo;

    // 1. Dashboard Summary
    public DashboardSummaryResponse getSummary() {
        List<Object[]> todayData = statRepo.getTodayStats();
        Long lowStock = statRepo.countLowStock();

        Object[] row = todayData.get(0);
        Double revenue = ((BigDecimal) row[0]).doubleValue();
        Long totalOrders = ((Number) row[1]).longValue();
        Long cancelled = ((Number) row[2]).longValue();

        Double cancelRate = totalOrders == 0 ? 0.0 : (double) cancelled / totalOrders * 100;

        return new DashboardSummaryResponse(revenue, totalOrders, lowStock, Math.round(cancelRate * 100.0) / 100.0);
    }

    // 2. Doanh thu Chart
    public List<ChartDataResponse> getRevenueChart(int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);

        return statRepo.getRevenueChart(start, end).stream()
                .map(r -> new ChartDataResponse(
                        (String) r[0],
                        ((BigDecimal) r[1]).doubleValue(), // Revenue
                        ((BigDecimal) r[2]).doubleValue()  // Profit
                )).collect(Collectors.toList());
    }

    // 3. Top Products
    public List<ProductStatResponse> getTopProducts(int limit) {
        return statRepo.getTopSellingProducts(limit).stream()
                .map(this::mapToProductStat)
                .collect(Collectors.toList());
    }

    // 4. Dead Stock
    public List<ProductStatResponse> getDeadStock() {
        return statRepo.getDeadStock().stream()
                .map(this::mapToProductStat)
                .collect(Collectors.toList());
    }

    // 5. Category Pie Chart
    public List<ChartDataResponse> getCategoryShare() {
        return statRepo.getCategoryShare().stream()
                .map(r -> new ChartDataResponse(
                        (String) r[0],
                        ((BigDecimal) r[1]).doubleValue(),
                        null
                )).collect(Collectors.toList());
    }

    // Helper map
    private ProductStatResponse mapToProductStat(Object[] row) {
        return new ProductStatResponse(
                ((Number) row[0]).longValue(), // id
                (String) row[1], // name
                (String) row[2], // sku
                ((Number) row[3]).longValue(), // sold
                ((Number) row[4]).longValue(), // stock
                ((BigDecimal) row[5]).doubleValue(), // revenue
                ((Number) row[6]).intValue() // days no sale
        );
    }
}
