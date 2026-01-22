package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.responses.*;
import com.manhduc205.ezgear.dtos.responses.reports.ChartDataResponse;
import com.manhduc205.ezgear.dtos.responses.reports.DashboardSummaryResponse;
import com.manhduc205.ezgear.dtos.responses.reports.ProductStatResponse;
import com.manhduc205.ezgear.services.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
public class StatisticController {

    private final StatisticService service;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<List<ChartDataResponse>> getRevenueChart(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(service.getRevenueChart(days));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<ProductStatResponse>> getTopProducts(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(service.getTopProducts(limit));
    }

    @GetMapping("/dead-stock")
    public ResponseEntity<List<ProductStatResponse>> getDeadStock() {
        return ResponseEntity.ok(service.getDeadStock());
    }

    @GetMapping("/category-share")
    public ResponseEntity<List<ChartDataResponse>> getCategoryShare() {
        return ResponseEntity.ok(service.getCategoryShare());
    }
}
