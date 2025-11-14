package com.xulunh.itemservice.web;

import com.xulunh.itemservice.domain.Item;
import com.xulunh.itemservice.dto.ItemDto;
import com.xulunh.itemservice.repository.ItemRepository;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemRepository itemRepository;
    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }
    @GetMapping("/by-upc")
    public ItemDto getByUpc(@RequestParam String upc) {
        Item i=itemRepository.findByUpc(upc).orElseThrow();
        return toDto(i);
    }

    @GetMapping("/{id}")
    public ItemDto getById(@PathVariable String id) {
        Item i= itemRepository.findById(id).orElseThrow();
        return toDto(i);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto create(@Valid @RequestBody ItemDto dto) {
        if (itemRepository.existsByUpc(dto.upc())){
            throw new IllegalStateException("Upc already exists");
        }
        Item i= toEntity(dto);
        Item newItem=itemRepository.save(i);
        return toDto(newItem);

    }
    @GetMapping
    public List<ItemDto> list() {
        return itemRepository.findAll().stream().map(this::toDto).toList();
    }

    @PatchMapping("/{id}/inventory")
    public ItemDto updateInventory(@PathVariable String id, @RequestParam int delta) {
        Item i = itemRepository.findById(id).orElseThrow();
        int newQty = Math.max(0, (i.getAvailableUnits() == null ? 0 : i.getAvailableUnits()) + delta);
        i.setAvailableUnits(newQty);
        return toDto(itemRepository.save(i));
    }

    private Item toEntity(ItemDto d) {
        Item i = new Item();
        i.setUpc(d.upc());
        i.setName(d.name());
        i.setUnitPrice(d.unitPrice());
        i.setPictureUrls(d.pictureUrls());
        i.setAvailableUnits(d.availableUnits());
        return i;
    }

    private ItemDto toDto(Item i) {
        return new ItemDto(i.getId(), i.getUpc(), i.getName(), i.getUnitPrice(), i.getPictureUrls(), i.getAvailableUnits());
    }
}
