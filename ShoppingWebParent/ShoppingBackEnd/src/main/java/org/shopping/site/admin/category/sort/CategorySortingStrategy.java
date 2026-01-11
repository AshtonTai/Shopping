package org.shopping.site.admin.category.sort;

import org.shopping.entity.Category;
import org.springframework.data.domain.Sort;
import java.util.Comparator;

public interface CategorySortingStrategy {
    Sort getSortForRootCategories();
    Comparator<Category> getComparatorForChildren();
}

