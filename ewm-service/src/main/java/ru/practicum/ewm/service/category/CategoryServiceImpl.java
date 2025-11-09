package ru.practicum.ewm.service.category;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.exception.AlreadyExistsException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import org.springframework.data.domain.Pageable;


import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventRepository eventRepository;

    // admin
    // добавление новой категории
    @Override
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new AlreadyExistsException("Категория уже существует:" + newCategoryDto.getName());
        }
        Category categoryToSave = categoryMapper.toCategory(newCategoryDto);
        categoryRepository.save(categoryToSave);
        return categoryMapper.toCategoryDto(categoryToSave);
    }

    // удаление категории
    @Override
    public void deleteCategory(Long categoryId) {
        if (eventRepository.existsByCategoryId(categoryId)) {
            throw new AlreadyExistsException("Категория не является пустой");
        }
        categoryRepository.deleteById(categoryId);
    }

    // изменение категории
    @Override
    public CategoryDto updateCategory(Long categoryId, NewCategoryDto categoryDto) {
        if (categoryDto.getName() == null || categoryDto.getName().isEmpty()) {
            throw new RuntimeException("Название категории не может быть пустым");
        }
        Category savedCategory = categoryRepository.findById(categoryId).orElseThrow(() ->
                new NotFoundException("Категория с идентификатором не найдена " + categoryId));
        if (!savedCategory.getName().equals(categoryDto.getName()) && categoryRepository.existsByName(categoryDto.getName())) {
            throw new AlreadyExistsException("Категория уже существует: " + categoryDto.getName());
        } else {
            savedCategory.setName(categoryDto.getName());
            return categoryMapper.toCategoryDto(categoryRepository.save(savedCategory));
        }
    }

    // public
    // получение категорий
    @Override
    public List<CategoryDto> getCategoryList(Pageable pageable) {
        return categoryMapper.toCategoryDtoList(categoryRepository.findAll(pageable).toList());
    }

    // получение инфо о категории по ее id
    @Override
    public CategoryDto getCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с идентификатором не найдена" + categoryId));
        return categoryMapper.toCategoryDto(category);
    }
}