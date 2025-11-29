package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.models.Shipment;

public interface ShipmentService {
    Shipment createShipment(Long orderId);
}
