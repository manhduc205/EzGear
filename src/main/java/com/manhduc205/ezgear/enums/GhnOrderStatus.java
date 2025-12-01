package com.manhduc205.ezgear.enums;

import lombok.Getter;

@Getter
public enum GhnOrderStatus {
    // Nhóm chờ lấy
    READY_TO_PICK("ready_to_pick", "Mới tạo, chờ lấy hàng"),
    PICKING("picking", "Nhân viên đang đi lấy hàng"),
    MONEY_COLLECT_PICKING("money_collect_picking", "Đang thu tiền người gửi"),

    // Nhóm đã lấy & luân chuyển
    PICKED("picked", "Đã lấy hàng thành công"),
    STORING("storing", "Hàng đang lưu kho"),
    TRANSPORTING("transporting", "Đang luân chuyển"),
    SORTING("sorting", "Đang phân loại hàng hóa"),

    // Nhóm giao hàng
    DELIVERING("delivering", "Shipper đang đi giao hàng"),
    MONEY_COLLECT_DELIVERING("money_collect_delivering", "Đang thu tiền người nhận"),
    DELIVERED("delivered", "Giao hàng thành công"),
    DELIVERY_FAIL("delivery_fail", "Giao hàng thất bại"),

    // Nhóm trả hàng
    WAITING_TO_RETURN("waiting_to_return", "Chờ xác nhận trả hàng"),
    RETURN("return", "Đang trả hàng"),
    RETURN_TRANSPORTING("return_transporting", "Đang luân chuyển hàng trả"),
    RETURN_SORTING("return_sorting", "Đang phân loại hàng trả"),
    RETURNING("returning", "Nhân viên đang đi trả hàng"),
    RETURN_FAIL("return_fail", "Trả hàng thất bại"),
    RETURNED("returned", "Trả hàng thành công (Shop đã nhận lại)"),

    // Nhóm lỗi/Hủy
    CANCEL("cancel", "Đơn hàng bị hủy"),
    EXCEPTION("exception", "Hàng ngoại lệ (Hư hỏng/Thất lạc)"),
    DAMAGE("damage", "Hàng bị hư hỏng"),
    LOST("lost", "Hàng bị thất lạc");

    private final String code;
    private final String description;

    GhnOrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    // Hàm tìm Enum theo code (VD: "ready_to_pick" -> READY_TO_PICK)
    public static GhnOrderStatus fromCode(String code) {
        for (GhnOrderStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null; // Hoặc throw exception
    }
}