package org.shopping.site.admin.user.sort;

import org.shopping.entity.User;
import org.springframework.data.domain.Sort;
import java.util.Comparator;

public class AscendingUserSortingStrategy implements UserSortingStrategy {

    @Override
    public Sort getSortForUsers(String sortField) {
        return Sort.by(sortField).ascending();
    }

    @Override
    public Comparator<User> getComparatorForInMemorySort(String sortField) {
        return switch (sortField.toLowerCase()) {
            case "id" -> Comparator.comparing(User::getId);
            case "email" -> Comparator.comparing(User::getEmail);
            case "firstname" -> Comparator.comparing(User::getFirstName);
            case "lastname" -> Comparator.comparing(User::getLastName);

            default -> Comparator.comparing(User::getFirstName); // fallback
        };
    }
}