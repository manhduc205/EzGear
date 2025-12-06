package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingCode(String trackingCode);
    Optional<Shipment> findByOrderId(Long orderId);
}
