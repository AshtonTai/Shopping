package org.shopping.site.admin.user.sort;

import org.springframework.stereotype.Component;

@Component
public class UserSortingStrategyFactory {

    public UserSortingStrategy getStrategy(String sortDir) {
        if ("desc".equalsIgnoreCase(sortDir)) {
            return new DescendingUserSortingStrategy();
        }
        return new AscendingUserSortingStrategy(); // default to asc
    }
}