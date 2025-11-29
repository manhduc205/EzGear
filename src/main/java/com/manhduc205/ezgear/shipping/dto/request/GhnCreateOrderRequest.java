package com.manhduc205.ezgear.shipping.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnCreateOrderRequest {

    // --- Thông tin người nhận (BẮT BUỘC) ---
    @JsonProperty("to_name")
    private String toName;

    @JsonProperty("to_phone")
    private String toPhone;

    @JsonProperty("to_address")
    private String toAddress; // Địa chỉ chi tiết

    @JsonProperty("to_ward_code")
    private String toWardCode;

    @JsonProperty("to_district_id")
    private Integer toDistrictId;
    //
    @JsonProperty("payment_type_id")
    private Integer paymentTypeId; // 1: Người bán trả ship, 2: Người mua trả

    @JsonProperty("note")
    private String note; // Ghi chú cho tài xế

    @JsonProperty("required_note")
    private String requiredNote; //  KHONGCHOXEMHANG, CHOXEMHANGKHONGTHU, CHOTHUHANG


    // --- Thông tin hàng hóa & COD ---
    @JsonProperty("cod_amount")
    private Integer codAmount; // Tiền thu hộ (VND)

    @JsonProperty("insurance_value")
    private Integer insuranceValue; // Giá trị bảo hiểm (thường = giá trị đơn hàng)

    // --- Kích thước & Trọng lượng (BẮT BUỘC) ---
    @JsonProperty("weight")
    private Integer weight; // Gram

    @JsonProperty("length")
    private Integer length; // CM

    @JsonProperty("width")
    private Integer width; // CM

    @JsonProperty("height")
    private Integer height; // CM

    // --- Dịch vụ vận chuyển ---
    @JsonProperty("service_id")
    private Integer serviceId; // ID gói dịch vụ (lấy từ API available-services)

    @JsonProperty("shop_id")
    private Integer shopId; // ID kho hàng trên GHN

    // --- Danh sách sản phẩm ---
    @JsonProperty("items")
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @JsonProperty("name")
        private String name;

        @JsonProperty("code")
        private String code; // SKU code

        @JsonProperty("quantity")
        private Integer quantity;

        @JsonProperty("price")
        private Integer price; // Giá sản phẩm

        @JsonProperty("weight")
        private Integer weight; // Trọng lượng từng món
    }
}