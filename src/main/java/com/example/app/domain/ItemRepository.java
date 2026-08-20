package com.example.app.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Thin JDBC repository for the {@code items} table.
 *
 * <p>Requires a {@link JdbcClient} which is auto-configured by Spring Boot
 * whenever a DataSource is present. In CI and in the {@code database=none}
 * deployment shape the Spring context is loaded with a {@code @MockBean} for
 * this repository so no real DataSource is needed.
 */
@Repository
public class ItemRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ItemRepository.class);

    private final JdbcClient jdbc;

    public ItemRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Returns all items ordered most-recent-first. */
    public List<Item> findAll() {
        return jdbc.sql(
                "SELECT id, name, created_at FROM items ORDER BY created_at DESC")
                .query(ItemRepository::mapRow)
                .list();
    }

    /** Returns a single item by primary key. */
    public Optional<Item> findById(long id) {
        return jdbc.sql(
                "SELECT id, name, created_at FROM items WHERE id = :id")
                .param("id", id)
                .query(ItemRepository::mapRow)
                .optional();
    }

    /**
     * Inserts a new item and returns it with the generated id and timestamp.
     *
     * @param name the item name; must not be null or blank
     */
    public Item create(String name) {
        LOG.debug("Creating item: name={}", name);
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO items (name) VALUES (:name)")
                .param("name", name)
                .update(keyHolder);
        long id = ((Number) keyHolder.getKeys().get("id")).longValue();
        return findById(id).orElseThrow(
                () -> new IllegalStateException("Item not found after insert: id=" + id));
    }

    /**
     * Updates the name of an existing item.
     *
     * @return the updated item, or empty if no row with that id exists
     */
    public Optional<Item> update(long id, String name) {
        LOG.debug("Updating item: id={}, name={}", id, name);
        int rows = jdbc.sql("UPDATE items SET name = :name WHERE id = :id")
                .param("name", name)
                .param("id", id)
                .update();
        if (rows == 0) {
            return Optional.empty();
        }
        return findById(id);
    }

    /**
     * Deletes an item.
     *
     * @return {@code true} if a row was deleted, {@code false} if the id did not exist
     */
    public boolean delete(long id) {
        LOG.debug("Deleting item: id={}", id);
        return jdbc.sql("DELETE FROM items WHERE id = :id")
                .param("id", id)
                .update() > 0;
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private static Item mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Item(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getTimestamp("created_at").toInstant());
    }
}
