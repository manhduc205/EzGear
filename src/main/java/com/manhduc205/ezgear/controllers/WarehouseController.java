package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.WarehouseDTO;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.services.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<Warehouse> create(@RequestBody WarehouseDTO dto) {
        return ResponseEntity.ok(warehouseService.createWarehouse(dto));
    }

    @GetMapping
    public ResponseEntity<List<WarehouseDTO>> getAll() {
        return ResponseEntity.ok(warehouseService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Warehouse> getById(@PathVariable Long id) {
        return warehouseService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Warehouse> update(@PathVariable Long id, @RequestBody WarehouseDTO dto) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
