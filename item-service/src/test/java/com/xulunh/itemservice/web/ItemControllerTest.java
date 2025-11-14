package com.xulunh.itemservice.web;

import com.xulunh.itemservice.domain.Item;
import com.xulunh.itemservice.dto.ItemDto;
import com.xulunh.itemservice.repository.ItemRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItemControllerTest {

    private final ItemRepository repo = mock(ItemRepository.class);
    private final ItemController controller = new ItemController(repo);

    private Item newItem(String id, String upc, String name, String price, Integer qty) {
        Item i = new Item();
        i.setId(id);
        i.setUpc(upc);
        i.setName(name);
        i.setUnitPrice(new BigDecimal(price));
        i.setAvailableUnits(qty);
        return i;
    }

    @Test
    void getByUpc_returnsDto() {
        when(repo.findByUpc("U1")).thenReturn(Optional.of(newItem("i1","U1","N", "2.50", 10)));
        var dto = controller.getByUpc("U1");
        assertThat(dto.upc()).isEqualTo("U1");
        assertThat(dto.availableUnits()).isEqualTo(10);
    }

    @Test
    void list_mapsAll() {
        when(repo.findAll()).thenReturn(List.of(
                newItem("i1","U1","N1","1.00", 1),
                newItem("i2","U2","N2","2.00", 2)
        ));
        var list = controller.list();
        assertThat(list).hasSize(2);
        assertThat(list.get(1).upc()).isEqualTo("U2");
    }

    @Test
    void create_throwsWhenUpcExists() {
        when(repo.existsByUpc("U1")).thenReturn(true);
        var dto = new ItemDto(null, "U1", "N", new BigDecimal("1.00"), List.of(), 5);
        assertThatThrownBy(() -> controller.create(dto)).isInstanceOf(IllegalStateException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void updateInventory_clampsToZero() {
        when(repo.findById("i1")).thenReturn(Optional.of(newItem("i1", "U1", "N", "1.00", 1)));
        when(repo.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        var res = controller.updateInventory("i1", -5);
        assertThat(res.availableUnits()).isEqualTo(0);
    }
}


