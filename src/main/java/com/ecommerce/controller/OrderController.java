package com.ecommerce.controller;

import com.ecommerce.model.entity.Order;
import com.ecommerce.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Body example: { "productId": 1, "quantity": 2 }
    @PostMapping
    public ResponseEntity<Order> placeOrder(@RequestBody Map<String, Object> body,
                                             HttpServletRequest httpRequest,
                                             Authentication authentication) {
        Long productId = Long.valueOf(body.get("productId").toString());
        Integer quantity = Integer.valueOf(body.get("quantity").toString());
        String tenantId = (String) httpRequest.getAttribute("tenantId");
        String customerEmail = authentication.getName();

        return ResponseEntity.ok(orderService.placeOrder(productId, quantity, customerEmail, tenantId));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Order>> myOrders(HttpServletRequest httpRequest, Authentication authentication) {
        String tenantId = (String) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(orderService.getMyOrders(authentication.getName(), tenantId));
    }

    @GetMapping
    public ResponseEntity<List<Order>> allOrders(HttpServletRequest httpRequest) {
        String tenantId = (String) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(orderService.getAllOrdersForTenant(tenantId));
    }
}
