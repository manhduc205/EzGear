package com.manhduc205.ezgear.repositories;


import com.manhduc205.ezgear.models.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatisticRepository extends JpaRepository<Order, Long> {

    // 1. Biểu đồ Doanh thu & Lợi nhuận theo ngày
    @Query(value = """
        SELECT 
            DATE_FORMAT(o.created_at, '%d/%m') as label,
            SUM(o.grand_total) as revenue,
            SUM(o.grand_total * 0.3) as profit -- Giả định lãi 30%
        FROM orders o
        WHERE o.status = 'COMPLETED' AND o.created_at BETWEEN :start AND :end
        GROUP BY DATE_FORMAT(o.created_at, '%d/%m'), DATE(o.created_at)
        ORDER BY DATE(o.created_at) ASC
    """, nativeQuery = true)
    List<Object[]> getRevenueChart(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 2. Top Sản phẩm bán chạy
    @Query(value = """
        SELECT p.id, p.name, sku.sku, SUM(oi.quantity) as sold, 
               (SELECT SUM(qty_on_hand) FROM product_stock WHERE sku_id = sku.id) as stock,
               SUM(oi.line_total) as revenue,
               0 as days_no_sale
        FROM order_items oi
        JOIN products p ON oi.product_id = p.id
        JOIN product_skus sku ON oi.sku_id = sku.id
        JOIN orders o ON oi.order_id = o.id
        WHERE o.status = 'COMPLETED'
        GROUP BY p.id, sku.id
        ORDER BY sold DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> getTopSellingProducts(@Param("limit") int limit);

    // 3. Tỷ lệ Category (Pie Chart)
    @Query(value = """
        SELECT c.name, SUM(oi.quantity) as val
        FROM order_items oi
        JOIN products p ON oi.product_id = p.id
        JOIN categories c ON p.category_id = c.id
        JOIN orders o ON oi.order_id = o.id
        WHERE o.status = 'COMPLETED'
        GROUP BY c.id
    """, nativeQuery = true)
    List<Object[]> getCategoryShare();

    // 4. Tổng quan Dashboard (Hôm nay)
    @Query(value = """
        SELECT 
            COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN grand_total ELSE 0 END), 0), -- Doanh thu
            COUNT(*), -- Tổng đơn
            COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END), 0) -- Đơn hủy
        FROM orders 
        WHERE DATE(created_at) = CURDATE()
    """, nativeQuery = true)
    List<Object[]> getTodayStats();

    // 5. Đếm số sản phẩm sắp hết hàng (Low Stock)
    @Query(value = "SELECT COUNT(*) FROM product_stock WHERE qty_on_hand <= safety_stock", nativeQuery = true)
    Long countLowStock();

    // 6. Tìm hàng tồn kho chết (Dead Stock > 90 ngày)
    @Query(value = """
        SELECT p.id, p.name, sku.sku, 0 as sold, 
               ps.qty_on_hand as stock,
               0 as revenue,
               DATEDIFF(NOW(), COALESCE(MAX(o.created_at), p.created_at)) as days_no_sale
        FROM product_stock ps
        JOIN product_skus sku ON ps.sku_id = sku.id
        JOIN products p ON sku.product_id = p.id
        LEFT JOIN order_items oi ON oi.sku_id = sku.id
        LEFT JOIN orders o ON oi.order_id = o.id
        GROUP BY sku.id, ps.id
        HAVING stock > 0 AND days_no_sale > 90
        LIMIT 10
    """, nativeQuery = true)
    List<Object[]> getDeadStock();
}
