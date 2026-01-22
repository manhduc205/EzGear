package com.manhduc205.ezgear.dtos.responses.reports;

import lombok.*;

@Data
@AllArgsConstructor
public class ChartDataResponse {
    private String label;  // Ngày tháng HOẶC Tên danh mục
    private Double value;  // Doanh thu HOẶC Số lượng
    private Double value2; // (Option) Lợi nhuận (nếu vẽ biểu đồ cột chồng)
}
