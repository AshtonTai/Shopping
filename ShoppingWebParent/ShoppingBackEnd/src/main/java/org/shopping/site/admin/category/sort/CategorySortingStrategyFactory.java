package org.shopping.site.admin.category.sort;

import org.springframework.stereotype.Component;

@Component
public class CategorySortingStrategyFactory {

    public CategorySortingStrategy getStrategy(String sortDir) {
        // Handle null, empty, or blank safely
        if (sortDir == null || sortDir.trim().isEmpty()) {
            return new AscendingCategorySortingStrategy(); // default
        }
        if ("desc".equalsIgnoreCase(sortDir.trim())) {
            return new DescendingCategorySortingStrategy();
        }
        return new AscendingCategorySortingStrategy(); // also treat invalid values as "asc"
    }
}
