package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.WarehouseDTO;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Warehouse;
import java.util.*;

public interface WarehouseService {
    Warehouse createWarehouse(WarehouseDTO dto);
    List<WarehouseDTO> getAll();
    Optional<Warehouse> getById(Long id);
    Warehouse updateWarehouse(Long id, WarehouseDTO dto);
    void delete(Long id);
    Long getWarehouseIdByAddress(CustomerAddress address);
}
