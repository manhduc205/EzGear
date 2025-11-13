package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.ProductStockDTO;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.responses.StockResponse;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.ProductStock;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.repositories.ProductStockRepository;
import com.manhduc205.ezgear.repositories.WarehouseRepository;
import com.manhduc205.ezgear.services.ProductStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
         return productStock.map(stock -> stock.getQtyOnHand() - stock.getQtyReserved() - stock.getSafetyStock())
                .orElse(0);
    }

    @Override
    @Transactional
    public void reduceStock(List<CartItemRequest> cartItems, Long orderId) {
        for (CartItemRequest item : cartItems) {
            ProductStockDTO dto = ProductStockDTO.builder()
                    .skuId(item.getSkuId())
                    .warehouseId(1L)
                    .build();
            adjustStock(dto, -item.getQuantity());
        }
    }

    @Override
    public List<StockResponse> getAllStock() {
        return productStockRepository.findAll()
                .stream()
                .map(stock -> StockResponse.builder()
                        .sku(stock.getProductSku().getSku())
                        .skuName(stock.getProductSku().getName())
                        .warehouseName(stock.getWarehouse().getName())
                        .qtyOnHand(stock.getQtyOnHand())
                        .qtyReserved(stock.getQtyReserved())
                        .safetyStock(stock.getSafetyStock())
                        .available(stock.getQtyOnHand()
                                - stock.getQtyReserved()
                                - stock.getSafetyStock())
                        .build()
                )
                .toList();
    }


}
