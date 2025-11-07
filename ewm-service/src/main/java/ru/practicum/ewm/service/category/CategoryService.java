package ru.practicum.ewm.service.category;


import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface CategoryService {

    CategoryDto addCategory(NewCategoryDto newCategoryDto);


    void deleteCategory(Long categoryId);


    CategoryDto updateCategory(Long categoryId, NewCategoryDto newCategoryDto);

    List<CategoryDto> getCategoryList(Pageable pageable);


    CategoryDto getCategory(Long categoryId);
}
