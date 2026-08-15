package com.ecommerce.service;

import com.ecommerce.model.entity.Order;
import com.ecommerce.model.entity.Product;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Transactional
    public Order placeOrder(Long productId, Integer quantity, String customerEmail, String tenantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (!product.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Product does not belong to your tenant");
        }
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("Not enough stock available");
        }

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        Order order = new Order();
        order.setProductId(productId);
        order.setCustomerEmail(customerEmail);
        order.setQuantity(quantity);
        order.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setTenantId(tenantId);

        Order saved = orderRepository.save(order);
        auditService.logSuccess("ORDER_PLACED", customerEmail, tenantId, "Order ID: " + saved.getId());
        return saved;
    }

    // Customers only see their own orders; admins see every order in the tenant.
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Transactional(readOnly = true)
    public List<Order> getMyOrders(String customerEmail, String tenantId) {
        return orderRepository.findByTenantIdAndCustomerEmail(tenantId, customerEmail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Order> getAllOrdersForTenant(String tenantId) {
        return orderRepository.findByTenantId(tenantId);
    }
}
