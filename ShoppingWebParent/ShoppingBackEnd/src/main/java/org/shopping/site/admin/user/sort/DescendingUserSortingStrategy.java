package org.shopping.site.admin.user.sort;

import org.shopping.entity.Role;
import org.shopping.entity.User;
import org.springframework.data.domain.Sort;

import java.util.Comparator;

public class DescendingUserSortingStrategy implements UserSortingStrategy {

    @Override
    public Sort getSortForUsers(String sortField) {
        return Sort.by(sortField).descending();
    }

    @Override
    public Comparator<User> getComparatorForInMemorySort(String sortField) {
        return switch (sortField.toLowerCase()) {
            case "id" -> Comparator.comparing(User::getId).reversed();
            case "email" -> Comparator.comparing(User::getEmail).reversed();
            case "firstname" -> Comparator.comparing(User::getFirstName).reversed();
            case "lastname" -> Comparator.comparing(User::getLastName).reversed();

            default -> Comparator.comparing(User::getFirstName).reversed();
        };
    }
}