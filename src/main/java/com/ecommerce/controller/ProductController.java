package com.ecommerce.controller;

import com.ecommerce.model.dto.ProductRequest;
import com.ecommerce.model.entity.Product;
import com.ecommerce.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Every endpoint here requires a valid JWT (see SecurityConfig).
 * The fine-grained "can this vendor touch this tenant's product?" checks happen
 * inside ProductService via @PreAuthorize, not here - controllers stay thin.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request,
                                           HttpServletRequest httpRequest,
                                           Authentication authentication) {
        String tenantId = (String) httpRequest.getAttribute("tenantId");
        String vendorEmail = authentication.getName();
        return ResponseEntity.ok(productService.createProduct(request, tenantId, vendorEmail));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping
    public ResponseEntity<List<Product>> listForMyTenant(HttpServletRequest httpRequest) {
        String tenantId = (String) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(productService.getProductsByTenant(tenantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
