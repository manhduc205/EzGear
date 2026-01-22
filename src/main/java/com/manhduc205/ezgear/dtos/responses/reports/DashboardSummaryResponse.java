package com.manhduc205.ezgear.dtos.responses.reports;

import lombok.*;

@Data
@AllArgsConstructor
public class DashboardSummaryResponse {
    private Double todayRevenue;    // Doanh thu hôm nay
    private Long newOrders;         // Đơn mới hôm nay
    private Long lowStockItems;     // Số SP sắp hết hàng
    private Double cancelRate;      // Tỷ lệ hủy đơn (%)
}
