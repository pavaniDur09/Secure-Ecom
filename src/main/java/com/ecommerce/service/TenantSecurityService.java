package com.ecommerce.service;

import com.ecommerce.model.entity.Product;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Small helper methods that @PreAuthorize annotations can call, e.g.:
 *   @PreAuthorize("@tenantSecurityService.hasAccessToProduct(#productId)")
 *
 * Spring Security lets you reference any Spring bean inside a @PreAuthorize
 * expression with "@beanName.method(...)" - that's what "tenantSecurityService" refers to
 * (Spring lowercases the first letter of the class name to make the bean name).
 */
@Service
@RequiredArgsConstructor
public class TenantSecurityService {

    private final ProductRepository productRepository;

    /** The tenantId of the currently logged-in user, read from the JWT (set by JwtAuthenticationFilter). */
    public String currentTenantId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        Object tenantId = attrs.getRequest().getAttribute("tenantId");
        return tenantId != null ? tenantId.toString() : null;
    }

    public String currentUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    /** True if the given tenantId matches the tenant the caller belongs to. */
    public boolean isSameTenant(String tenantId) {
        String current = currentTenantId();
        return current != null && current.equals(tenantId);
    }

    /** A vendor may only manage products inside their own tenant. */
    public boolean isCurrentUserTenantAdmin(String tenantId) {
        return isSameTenant(tenantId);
    }

    /** A user may only view/edit a product that belongs to their own tenant. */
    public boolean hasAccessToProduct(Long productId) {
        return productRepository.findById(productId)
                .map(Product::getTenantId)
                .map(this::isSameTenant)
                .orElse(false);
    }
}
