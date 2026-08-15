package com.ecommerce.service;

import com.ecommerce.model.entity.Tenant;
import com.ecommerce.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Only global admins manage tenants (shops) - creating a new tenant is like
 * onboarding a brand-new store onto the platform.
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Tenant createTenant(String name, String description) {
        if (tenantRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Tenant already exists: " + name);
        }
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setDescription(description);
        return tenantRepository.save(tenant);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }
}
