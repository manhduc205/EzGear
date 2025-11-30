package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.ShipmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentHistoryRepository extends JpaRepository<ShipmentHistory, Long> {
    List<ShipmentHistory> findByShipmentIdOrderByEventTimeDesc(Long shipmentId);
}