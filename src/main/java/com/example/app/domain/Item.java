package com.example.app.domain;

import java.time.Instant;

/**
 * Immutable value object representing an item row returned from the database.
 *
 * <p>Spring JDBC maps result sets to this record via
 * {@link org.springframework.jdbc.core.simple.JdbcClient} or
 * {@link org.springframework.jdbc.core.RowMapper}. It is not a JPA entity —
 * the service uses plain JDBC to avoid introducing Hibernate to a project
 * that does not need an ORM.
 */
public record Item(Long id, String name, Instant createdAt) {
}
