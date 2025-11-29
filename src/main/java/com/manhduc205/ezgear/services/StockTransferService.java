package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.StockTransferRequest;
import com.manhduc205.ezgear.dtos.responses.StockTransferResponse;
import com.manhduc205.ezgear.models.StockTransfer;

import java.util.List;

public interface StockTransferService {
    StockTransfer createTransfer(StockTransferRequest req, Long userId);
    void createAutoTransfer(Long fromWhId, Long toWhId, Long skuId, int qty, String refOrderCode);
    void shipTransfer(Long transferId);
    void receiveTransfer(Long transferId);
    List<StockTransferResponse> getAll();
}
