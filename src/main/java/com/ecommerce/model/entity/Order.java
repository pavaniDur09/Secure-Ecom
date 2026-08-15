package com.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String customerEmail; // who placed the order

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private String tenantId;

    private LocalDateTime createdAt = LocalDateTime.now();
}
