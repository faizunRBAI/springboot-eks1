package com.example.app;

import com.example.app.domain.Item;
import com.example.app.domain.ItemNotFoundException;
import com.example.app.domain.ItemRepository;
import com.example.app.domain.ItemService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ItemService} using Mockito stubs.
 *
 * <p>No Spring context is started — this class tests business logic only.
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository repository;

    private ItemService service;

    @BeforeEach
    void setUp() {
        service = new ItemService(repository);
    }

    @Test
    void listAllDelegatesToRepository() {
        List<Item> expected = List.of(new Item(1L, "alpha", Instant.now()));
        when(repository.findAll()).thenReturn(expected);

        assertThat(service.listAll()).isEqualTo(expected);
    }

    @Test
    void getByIdReturnsItemWhenFound() {
        Item item = new Item(42L, "beta", Instant.now());
        when(repository.findById(42L)).thenReturn(Optional.of(item));

        assertThat(service.getById(42L)).isEqualTo(item);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createPassesNormalisedNameToRepository() {
        Item created = new Item(1L, "trimmed", Instant.now());
        when(repository.create("trimmed")).thenReturn(created);

        assertThat(service.create("  trimmed  ")).isEqualTo(created);
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> service.create("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsNameExceeding255Chars() {
        String longName = "a".repeat(256);
        assertThatThrownBy(() -> service.create(longName))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateThrowsWhenItemNotFound() {
        when(repository.update(anyLong(), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(7L, "new name"))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("7");
    }

    @Test
    void deleteThrowsWhenItemNotFound() {
        when(repository.delete(anyLong())).thenReturn(false);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("5");
    }
}
