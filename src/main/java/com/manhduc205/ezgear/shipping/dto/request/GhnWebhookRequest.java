package com.manhduc205.ezgear.shipping.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnWebhookRequest {

    @JsonProperty("OrderCode")
    private String orderCode;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Time")
    private String time;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("Reason")
    private String reason;

    @JsonProperty("Warehouse")
    private String warehouse;
}
