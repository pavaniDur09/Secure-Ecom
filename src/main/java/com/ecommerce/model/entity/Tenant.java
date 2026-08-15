package com.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A "Tenant" = one shop/store on our multi-tenant platform.
 *
 * SIMPLIFIED MULTI-TENANCY:
 * The real spec asks for a separate database per tenant (row-per-tenant-DB routing).
 * That's powerful but heavy to learn from. Here we use the much simpler and very common
 * approach: ONE shared database, and every table that belongs to a tenant
 * (User, Product, Order) just has a `tenantId` column. This is called
 * "row-level multi-tenancy" and is what most SaaS apps actually start with.
 */
@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. "tenant1"

    private String description;
}
