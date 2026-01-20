package org.shopping.site.admin.user.sort;

import org.shopping.entity.User;
import org.springframework.data.domain.Sort;

import java.util.Comparator;

public interface UserSortingStrategy {
    Sort getSortForUsers(String sortField);
    Comparator<User> getComparatorForInMemorySort(String sortField);
}
