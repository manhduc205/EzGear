package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.request.InvoiceSearchRequest;
import com.manhduc205.ezgear.dtos.responses.invoice.InvoiceResponse;
import com.manhduc205.ezgear.models.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceService {
    Invoice createInvoice(Long orderId);
    InvoiceResponse getInvoiceById(Long id);
    InvoiceResponse getInvoiceByOrderId(Long orderId);
    void markInvoiceAsPaid(Long invoiceId);
    Page<InvoiceResponse> searchInvoices(InvoiceSearchRequest request, Pageable pageable);
}