package org.shopping.site.admin;

import org.shopping.site.admin.security.ShoppingUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component // or @Service if you prefer
public class BaseController {
        protected Integer getCurrentUserId(Authentication auth) { // ← changed to protected
            if (auth == null || !auth.isAuthenticated()) {
                throw new RuntimeException("User not authenticated");
            }
            return ((ShoppingUserDetails) auth.getPrincipal()).getId();
        }
}