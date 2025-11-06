package service.adminService;


import dto.category.CategoryDto;
import dto.category.NewCategoryDto;
import exception.ConflictException;
import exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import model.Category;
import org.springframework.stereotype.Service;
import repository.CategoryRepository;
import repository.EventRepository;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    public CategoryDto addCategory(NewCategoryDto dto) {
        Category category = Category.builder().name(dto.getName()).build();
        category = categoryRepository.save(category);
        return toDto(category);
    }

    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        category.setName(dto.getName());
        category = categoryRepository.save(category);
        return toDto(category);
    }

    public void deleteCategory(Long catId) {
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.deleteById(catId);
    }

    private CategoryDto toDto(Category c) {
        return CategoryDto.builder().id(c.getId()).name(c.getName()).build();
    }
}
