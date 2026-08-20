package com.example.app.domain;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Application-layer service for item operations.
 *
 * <p>Only registered when {@link ItemRepository} is available — which requires a
 * {@link org.springframework.jdbc.core.simple.JdbcClient} bean (i.e. a configured
 * DataSource). The service starts stateless without a database, and the Items
 * API is absent until one is connected.
 */
@Service
@ConditionalOnBean(ItemRepository.class)
public class ItemService {

    private final ItemRepository repository;

    public ItemService(ItemRepository repository) {
        this.repository = repository;
    }

    /** Returns all items, newest first. */
    public List<Item> listAll() {
        return repository.findAll();
    }

    /**
     * Returns a single item.
     *
     * @throws ItemNotFoundException if no item with that id exists
     */
    public Item getById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
    }

    /**
     * Creates a new item.
     *
     * @throws IllegalArgumentException if name is blank
     */
    public Item create(String name) {
        validateName(name);
        return repository.create(name.trim());
    }

    /**
     * Updates an existing item's name.
     *
     * @throws ItemNotFoundException if no item with that id exists
     * @throws IllegalArgumentException if name is blank
     */
    public Item update(long id, String name) {
        validateName(name);
        return repository.update(id, name.trim())
                .orElseThrow(() -> new ItemNotFoundException(id));
    }

    /**
     * Deletes an item.
     *
     * @throws ItemNotFoundException if no item with that id exists
     */
    public void delete(long id) {
        if (!repository.delete(id)) {
            throw new ItemNotFoundException(id);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void validateName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (name.trim().length() > 255) {
            throw new IllegalArgumentException("name must be 255 characters or fewer");
        }
    }
}
