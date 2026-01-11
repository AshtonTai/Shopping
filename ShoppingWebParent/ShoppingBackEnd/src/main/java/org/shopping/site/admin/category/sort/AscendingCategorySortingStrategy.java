package org.shopping.site.admin.category.sort;

import org.shopping.entity.Category;
import org.springframework.data.domain.Sort;

import java.util.Comparator;

public class AscendingCategorySortingStrategy implements CategorySortingStrategy {
    @Override
    public Sort getSortForRootCategories() {
        return Sort.by("name").ascending();
    }

    @Override
    public Comparator<Category> getComparatorForChildren() {
        return Comparator.comparing(Category::getName);
    }
}
