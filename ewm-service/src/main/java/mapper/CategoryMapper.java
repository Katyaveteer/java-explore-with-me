package mapper;


import dto.category.CategoryDto;
import dto.category.NewCategoryDto;
import model.Category;
import org.springframework.stereotype.Component;


@Component
public class CategoryMapper {

    public CategoryDto toCategoryDto(Category category) {
        if (category == null) {
            return null;
        }

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(category.getId());
        categoryDto.setName(category.getName());

        return categoryDto;
    }

    public Category toCategory(NewCategoryDto newCategoryDto) {
        if (newCategoryDto == null) {
            return null;
        }

        Category category = new Category();
        category.setName(newCategoryDto.getName());

        return category;
    }

    public Category toCategory(CategoryDto categoryDto) {
        if (categoryDto == null) {
            return null;
        }

        Category category = new Category();
        category.setId(categoryDto.getId());
        category.setName(categoryDto.getName());

        return category;
    }
}