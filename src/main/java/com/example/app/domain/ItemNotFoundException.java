package com.example.app.domain;

/** Thrown when a requested item does not exist in the database. */
public class ItemNotFoundException extends RuntimeException {

    private final long itemId;

    public ItemNotFoundException(long itemId) {
        super("Item not found: id=" + itemId);
        this.itemId = itemId;
    }

    public long getItemId() {
        return itemId;
    }
}
