package com.taekwondogym.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.taekwondogym.backend.model.Order;
import com.taekwondogym.backend.model.User;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByOrderItemsProductId(Long productId);
    Optional<Order> findById(Long orderId);
    
    
    @Query("SELECT FUNCTION('MONTH', o.orderDate) AS month, " +
            "SUM(oi.price * oi.quantity) AS totalSales " +
            "FROM Order o " +
            "JOIN o.orderItems oi " +
            "WHERE (:productId IS NULL OR oi.product.id = :productId) " +
            "GROUP BY FUNCTION('MONTH', o.orderDate)")
     List<Object[]> getMonthlySalesSummary(@Param("productId") Long productId);
}