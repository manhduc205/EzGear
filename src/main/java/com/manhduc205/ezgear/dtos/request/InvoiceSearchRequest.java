package com.manhduc205.ezgear.dtos.request;

import com.manhduc205.ezgear.enums.InvoiceStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InvoiceSearchRequest {
    private String code;
    private String orderCode;
    private String customerName;
    private InvoiceStatus status;
    private LocalDate fromDate;
    private LocalDate toDate;
}