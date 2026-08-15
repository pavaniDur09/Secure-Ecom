package com.ecommerce.controller;

import com.ecommerce.model.entity.Tenant;
import com.ecommerce.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    // Body example: { "name": "tenant1", "description": "Jane's Bakery" }
    @PostMapping
    public ResponseEntity<Tenant> create(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(tenantService.createTenant(body.get("name"), body.get("description")));
    }

    @GetMapping
    public ResponseEntity<List<Tenant>> list() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }
}
