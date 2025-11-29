package com.manhduc205.ezgear.enums;

public enum TransferStatus {
    PENDING,    // Mới tạo, chờ duyệt (Hàng đã được Reserve ở kho nguồn)
    SHIPPING,   // Đang vận chuyển (Đã trừ kho thật ở nguồn)
    COMPLETED,  // Đã nhập kho đích (Cộng kho ở đích)
    CANCELLED   // Hủy (Trả lại Reserve cho kho nguồn)
}
