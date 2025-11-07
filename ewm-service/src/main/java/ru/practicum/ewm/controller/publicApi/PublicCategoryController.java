package ru.practicum.ewm.controller.publicApi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import ru.practicum.ewm.dto.category.CategoryDto;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.category.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PublicCategoryController {
    private final CategoryService categoryService;


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryDto> getCategoryList(@RequestParam(required = false, defaultValue = "0") @PositiveOrZero Integer from,
                                             @RequestParam(required = false, defaultValue = "10") @Positive Integer size) {
        log.info("PubCategoryController / getCategoryList: получение категорий " + from + size);
        return categoryService.getCategoryList(PageRequest.of(from, size));
    }


    @GetMapping("/{catId}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryDto getCategory(@PathVariable Long catId) {
        log.info("PubCategoryController / getCategory: получение инфо о категории по ее id " + catId);
        return categoryService.getCategory(catId);
    }
}