package com.example.app.web;

import com.example.app.domain.Item;
import com.example.app.domain.ItemService;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for the {@code /api/items} resource.
 *
 * <p>All error mapping lives in {@link ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    /** {@code GET /api/items} — list all items, newest first. */
    @GetMapping
    public List<Item> list() {
        return service.listAll();
    }

    /** {@code GET /api/items/{id}} — fetch one item. */
    @GetMapping("/{id}")
    public Item getOne(@PathVariable long id) {
        return service.getById(id);
    }

    /** {@code POST /api/items} — create an item; returns 201 with a Location header. */
    @PostMapping
    public ResponseEntity<Item> create(@RequestBody Map<String, String> body) {
        Item created = service.create(body.get("name"));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** {@code PUT /api/items/{id}} — replace the name of an existing item. */
    @PutMapping("/{id}")
    public Item update(@PathVariable long id, @RequestBody Map<String, String> body) {
        return service.update(id, body.get("name"));
    }

    /** {@code DELETE /api/items/{id}} — remove an item; returns 204. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
