package com.ecommerce.service;

import com.ecommerce.model.dto.ProductRequest;
import com.ecommerce.model.entity.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @PreAuthorize checks run BEFORE the method body executes. If the expression evaluates
 * to false, Spring throws an AccessDeniedException automatically - the method body never runs.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final TenantSecurityService tenantSecurityService;
    private final AuditService auditService;

    // Admins can create in any tenant; vendors only in their own tenant.
    @PreAuthorize("hasRole('ADMIN') or (hasRole('VENDOR') and @tenantSecurityService.isSameTenant(#tenantId))")
    @Transactional
    public Product createProduct(ProductRequest request, String tenantId, String vendorEmail) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setTenantId(tenantId);
        product.setVendorEmail(vendorEmail);

        Product saved = productRepository.save(product);
        auditService.logSuccess("PRODUCT_CREATED", vendorEmail, tenantId, "Product ID: " + saved.getId());
        return saved;
    }

    // Anyone can view a product, but only from their own tenant (or if they're a global admin).
    @PreAuthorize("hasRole('ADMIN') or @tenantSecurityService.hasAccessToProduct(#productId)")
    @Transactional(readOnly = true)
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR', 'CUSTOMER')")
    @Transactional(readOnly = true)
    public List<Product> getProductsByTenant(String tenantId) {
        return productRepository.findByTenantId(tenantId);
    }

    @PreAuthorize("hasRole('ADMIN') or @tenantSecurityService.hasAccessToProduct(#productId)")
    @Transactional
    public Product updateProduct(Long productId, ProductRequest request) {
        Product product = getProduct(productId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        return productRepository.save(product);
    }

    @PreAuthorize("hasRole('ADMIN') or @tenantSecurityService.hasAccessToProduct(#productId)")
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = getProduct(productId);
        productRepository.delete(product);
        auditService.logSuccess("PRODUCT_DELETED", tenantSecurityService.currentUserEmail(),
                product.getTenantId(), "Product ID: " + productId);
    }
}
