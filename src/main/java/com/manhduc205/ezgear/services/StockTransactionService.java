package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.StockTransactionReportDTO;

import java.util.List;

public interface StockTransactionService {
    List<StockTransactionReportDTO> getTransactionReports(Long skuId, Long warehouseId);

}
