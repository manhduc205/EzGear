package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.ProductStockDTO;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.ProductStock;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.repositories.ProductStockRepository;
import com.manhduc205.ezgear.repositories.WarehouseRepository;
import com.manhduc205.ezgear.services.ProductStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductStockServiceImpl implements ProductStockService {

    private final ProductStockRepository productStockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductSkuRepository productSkuRepository;

    //Input: ProductStockDTO + delta (số lượng cộng/trừ).
    @Override
    public ProductStockDTO adjustStock(ProductStockDTO productStockDTO, int delta) {
        ProductStock productStock = productStockRepository.findByProductSkuIdAndWarehouseId(productStockDTO.getSkuId(),productStockDTO.getWarehouseId())
                .orElseGet(() ->{
                    ProductSKU sku = productSkuRepository.findById(productStockDTO.getSkuId())
                            .orElseThrow(() -> new RuntimeException("SKU not found"));
                    Warehouse warehouse = warehouseRepository.findById(productStockDTO.getWarehouseId())
                            .orElseThrow(() -> new RuntimeException("Warehouse not found"));
                    return ProductStock.builder()
                            .productSku(sku)
                            .warehouse(warehouse)
                            .qtyOnHand(0)
                            .qtyReserved(0)
                            .safetyStock(productStockDTO.getSafetyStock() != null ? productStockDTO.getSafetyStock() : 0)
                            .build();
                });
        productStock.setQtyOnHand(productStock.getQtyOnHand() + delta);
        ProductStock saved = productStockRepository.save(productStock);
        return ProductStockDTO.builder()
                .id(saved.getId())
                .skuId(saved.getProductSku().getId())
                .warehouseId(saved.getWarehouse().getId())
                .qtyOnHand(saved.getQtyOnHand())
                .qtyReserved(saved.getQtyReserved())
                .safetyStock(saved.getSafetyStock())
                .build();
    }

    @Override
    public int getAvailable(Long skuId, Long warehouseId) {
        Optional<ProductStock> productStock = productStockRepository.findByProductSkuIdAndWarehouseId(skuId,warehouseId);
         return productStock.map(stock -> stock.getQtyOnHand() - stock.getQtyReserved())
                .orElse(0);
    }
}
