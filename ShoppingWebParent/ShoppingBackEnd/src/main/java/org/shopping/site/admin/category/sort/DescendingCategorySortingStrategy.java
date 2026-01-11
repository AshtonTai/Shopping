package org.shopping.site.admin.category.sort;

import org.shopping.entity.Category;
import org.springframework.data.domain.Sort;

import java.util.Comparator;

public class DescendingCategorySortingStrategy implements CategorySortingStrategy {
    @Override
    public Sort getSortForRootCategories() {
        return Sort.by("name").descending();
    }

    @Override
    public Comparator<Category> getComparatorForChildren() {
        return (c1, c2) -> c2.getName().compareTo(c1.getName());
    }
}
