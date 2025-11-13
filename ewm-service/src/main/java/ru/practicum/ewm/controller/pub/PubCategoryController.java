package ru.practicum.ewm.controller.pub;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.service.category.CategoryService;

import java.util.List;

@RestController
@RequestMapping(path = "/categories")
@RequiredArgsConstructor
@Slf4j
public class PubCategoryController {
    private final CategoryService categoryService;

    // получение категорий
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryDto> getCategoryList(@RequestParam(required = false, defaultValue = "0") @PositiveOrZero Integer from,
                                             @RequestParam(required = false, defaultValue = "10") @Positive Integer size) {
        log.info("PubCategoryController / getCategoryList: получение категорий " + from + size);
        return categoryService.getCategoryList(PageRequest.of(from, size));
    }

    // получение инфо о категории по ее id
    @GetMapping("/{catId}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryDto getCategory(@PathVariable Long catId) {
        log.info("PubCategoryController / getCategory: получение инфо о категории по ее id " + catId);
        return categoryService.getCategory(catId);
    }
}