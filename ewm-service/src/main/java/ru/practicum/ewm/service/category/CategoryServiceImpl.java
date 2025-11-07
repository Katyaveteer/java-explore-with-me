package ru.practicum.ewm.service.category;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.exception.AlreadyExistsException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventRepository eventRepository;


    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new AlreadyExistsException("Category already exists: " + newCategoryDto.getName());
        }
        Category categoryToSave = categoryMapper.toCategory(newCategoryDto);
        categoryRepository.save(categoryToSave);
        return categoryMapper.toCategoryDto(categoryToSave);
    }


    @Override
    public void deleteCategory(Long categoryId) {
        if (eventRepository.existsByCategoryId(categoryId)) {
            throw new AlreadyExistsException("Category is not empty");
        }
        categoryRepository.deleteById(categoryId);
    }


    @Override
    public CategoryDto updateCategory(Long categoryId, NewCategoryDto categoryDto) {
        if (categoryDto.getName() == null || categoryDto.getName().isEmpty()) {
            throw new RuntimeException("Category name could not be empty");
        }
        Category savedCategory = categoryRepository.findById(categoryId).orElseThrow(() ->
                new NotFoundException("Category was not found with id " + categoryId));
        if (!savedCategory.getName().equals(categoryDto.getName()) && categoryRepository.existsByName(categoryDto.getName())) {
            throw new AlreadyExistsException("Category already exists: " + categoryDto.getName());
        } else {
            savedCategory.setName(categoryDto.getName());
            return categoryMapper.toCategoryDto(categoryRepository.save(savedCategory));
        }
    }


    @Override
    public List<CategoryDto> getCategoryList(Pageable pageable) {
        return categoryMapper.toCategoryDtoList(categoryRepository.findAll(pageable).toList());
    }

    @Override
    public CategoryDto getCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category was not found with id " + categoryId));
        return categoryMapper.toCategoryDto(category);
    }
}