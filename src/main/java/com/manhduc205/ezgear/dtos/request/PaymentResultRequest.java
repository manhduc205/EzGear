package com.manhduc205.ezgear.dtos.request;

import lombok.Data;

@Data
public class PaymentResultRequest {
    private String vnp_TmnCode;
    private String vnp_Amount;
    private String vnp_BankCode;
    private String vnp_OrderInfo;
    private String vnp_ResponseCode;
    private String vnp_TransactionNo;
    private String vnp_TxnRef;
    private String vnp_SecureHash;
    private String vnp_TransactionStatus;
}
