package com.manhduc205.ezgear.shipping.location_ghn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GhnLocationDTO {
    private String id;
    private String name;
    private GhnLocation.Type type;
    private String parentId;
}

