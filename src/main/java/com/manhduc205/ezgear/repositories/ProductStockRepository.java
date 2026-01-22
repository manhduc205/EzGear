package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.ProductStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {
    Optional<ProductStock> findByProductSkuIdAndWarehouseId(Long skuId, Long warehouseId);

    // Increase reserved if available (qty_on_hand - qty_reserved - safety_stock) >= :qty
    @Modifying
    @Query("UPDATE ProductStock ps SET ps.qtyReserved = ps.qtyReserved + :qty " +
            "WHERE ps.productSku.id = :skuId AND ps.warehouse.id = :warehouseId " +
            "AND (ps.qtyOnHand - ps.qtyReserved - ps.safetyStock) >= :qty")
    int reserveStock(@Param("skuId") Long skuId,
                     @Param("warehouseId") Long warehouseId,
                     @Param("qty") int qty);

    // Decrease qty_on_hand and reserved when committing reservation
    @Modifying
    @Query("UPDATE ProductStock ps SET ps.qtyOnHand = ps.qtyOnHand - :qty, ps.qtyReserved = ps.qtyReserved - :qty " +
            "WHERE ps.productSku.id = :skuId AND ps.warehouse.id = :warehouseId " +
            "AND ps.qtyReserved >= :qty")
    int commitReserved(@Param("skuId") Long skuId,
                       @Param("warehouseId") Long warehouseId,
                       @Param("qty") int qty);

    // Release reservation only (used on payment fail/expire)
    @Modifying
    @Query("UPDATE ProductStock ps SET ps.qtyReserved = ps.qtyReserved - :qty " +
            "WHERE ps.productSku.id = :skuId AND ps.warehouse.id = :warehouseId " +
            "AND ps.qtyReserved >= :qty")
    int releaseReserved(@Param("skuId") Long skuId,
                        @Param("warehouseId") Long warehouseId,
                        @Param("qty") int qty);

    // Direct stock reduction without reservation (for COD)
    @Modifying
    @Query("UPDATE ProductStock ps SET ps.qtyOnHand = ps.qtyOnHand - :qty " +
            "WHERE ps.productSku.id = :skuId AND ps.warehouse.id = :warehouseId " +
            "AND (ps.qtyOnHand - ps.qtyReserved - ps.safetyStock) >= :qty")
    int reduceDirect(@Param("skuId") Long skuId,
                     @Param("warehouseId") Long warehouseId,
                     @Param("qty") int qty);
    // tính tổng tồn kho trên toàn quốc
    @Query("SELECT COALESCE(SUM(ps.qtyOnHand - ps.qtyReserved - ps.safetyStock), 0) " +
            "FROM ProductStock ps WHERE ps.productSku.id = :skuId")
    Integer sumTotalAvailable(@Param("skuId") Long skuId);
    // tính tổng tồn kho trong một tỉnh/thành
    @Query("SELECT COALESCE(SUM(ps.qtyOnHand - ps.qtyReserved - ps.safetyStock), 0) " +
            "FROM ProductStock ps " +
            "JOIN ps.warehouse w " +
            "JOIN w.branch b " +
            "WHERE ps.productSku.id = :skuId " +
            "AND b.provinceId = :provinceId " +
            "AND w.isActive = true " +
            "AND b.isActive = true")
    Integer sumStockByProvince(@Param("skuId") Long skuId, @Param("provinceId") Integer provinceId);

    @Modifying
    @Query("UPDATE ProductStock ps SET ps.qtyOnHand = ps.qtyOnHand + :qty " +
            "WHERE ps.productSku.id = :skuId AND ps.warehouse.id = :warehouseId")
    int increaseStock(@Param("skuId") Long skuId, @Param("warehouseId") Long warehouseId, @Param("qty") int qty);

    // lấy tồn kho tồn kho dựa trên Branch ID
    @Query("SELECT ps FROM ProductStock ps WHERE ps.warehouse.branch.id = :branchId")
    List<ProductStock> findAllByBranchId(@Param("branchId") Long branchId);

    // Lấy tất cả tồn kho của list SKU trong list Kho
    @Query("SELECT ps FROM ProductStock ps " +
            "WHERE ps.productSku.id IN :skuIds " +
            "AND ps.warehouse.id IN :warehouseIds")
    List<ProductStock> findAllBySkuIdInAndWarehouseIdIn(
            @Param("skuIds") List<Long> skuIds,
            @Param("warehouseIds") List<Long> warehouseIds
    );
    @Query("SELECT ps FROM ProductStock ps " +
            "JOIN ps.warehouse w " +
            "JOIN w.branch b " +
            "WHERE ps.productSku.id = :skuId " +
            "AND b.provinceId = :provinceId " +
            "AND w.isActive = true " +
            "AND (ps.qtyOnHand - ps.qtyReserved - ps.safetyStock) > 0 " +
            "ORDER BY (ps.qtyOnHand - ps.qtyReserved - ps.safetyStock) DESC")
    List<ProductStock> findAvailableInProvince(@Param("skuId") Long skuId,
                                               @Param("provinceId") Integer provinceId);

    @Query("""
            SELECT ps FROM ProductStock ps
            JOIN FETCH ps.warehouse w
            JOIN FETCH w.branch b
            WHERE ps.productSku.id = :skuId
                AND ps.qtyOnHand > 0
                AND w.isActive = true
                AND b.isActive = true
                AND (:provinceId IS NULL OR b.provinceId = :provinceId)
                AND(:districtId IS NULL OR b.districtId = :districtId)
            ORDER BY (ps.qtyOnHand - COALESCE(ps.qtyReserved, 0)) DESC
""")
    List<ProductStock> findStockLocations(@Param("skuId") Long skuId,
                                               @Param("provinceId") Integer provinceId,
                                               @Param("districtId") Integer districtId);
    
}

