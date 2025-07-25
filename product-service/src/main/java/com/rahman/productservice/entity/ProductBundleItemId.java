package com.rahman.productservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Embeddable
public class ProductBundleItemId implements Serializable {
    private static final long serialVersionUID = -4086535575189118834L;
    @NotNull
    @Column(name = "bundle_id", nullable = false)
    private UUID bundleId;

    @NotNull
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ProductBundleItemId entity = (ProductBundleItemId) o;
        return Objects.equals(this.productId, entity.productId) &&
                Objects.equals(this.bundleId, entity.bundleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, bundleId);
    }

}