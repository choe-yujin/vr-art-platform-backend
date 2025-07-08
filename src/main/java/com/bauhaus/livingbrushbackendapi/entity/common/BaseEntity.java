package com.bauhaus.livingbrushbackendapi.entity.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * All entities inherit common timestamp fields from this base class.
 * This class uses Hibernate annotations to leverage the database's automatic
 * timestamp generation capabilities, aligning with the DDL script.
 */
@Getter
@MappedSuperclass // Specifies that this is a base class for entities and not an entity itself.
public abstract class BaseEntity {

    /**
     * The timestamp when the entity was created.
     * @CreationTimestamp lets Hibernate manage this, expecting the DB to set the value.
     * updatable = false ensures this value is never changed after creation.
     * OffsetDateTime is the correct Java type for the SQL 'TIMESTAMP WITH TIME ZONE'.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * The timestamp when the entity was last updated.
     * @UpdateTimestamp lets Hibernate manage this, expecting the DB to update the value.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}