package org.shopping.site.admin.user;

import jakarta.transaction.Transactional;
import org.shopping.entity.Role;
import org.shopping.entity.User;
import org.shopping.site.admin.paging.PagingAndSortingHelper;
import org.shopping.site.admin.user.sort.UserSortingStrategy;
import org.shopping.site.admin.user.sort.UserSortingStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    public static final int USERS_PER_PAGE = 4;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserSortingStrategyFactory sortingStrategyFactory; // ← NEW

    public User getByEmail(String email) {
        return userRepo.getUserByEmail(email);
    }

    public List<User> listAll() {
        return userRepo.findAll(org.springframework.data.domain.Sort.by("firstName").ascending());
    }

    public Page<User> listByPage(int pageNum, PagingAndSortingHelper helper) {
        String sortField = helper.getSortField();
        String sortDir = helper.getSortDir();
        String keyword = helper.getKeyword();

        String normalizedSortField = normalizeSortField(sortField); // ensures safety

        UserSortingStrategy strategy = sortingStrategyFactory.getStrategy(sortDir);
        Sort sort = strategy.getSortForUsers(normalizedSortField);
        Pageable pageable = PageRequest.of(pageNum - 1, USERS_PER_PAGE, sort);

        if (keyword != null && !keyword.trim().isEmpty()) {
            return userRepo.findAll(keyword, pageable);
        } else {
            return userRepo.findAll(pageable);
        }
    }

    // Helper to sanitize and validate sort field
    private String normalizeSortField(String sortField) {
        if (sortField == null || sortField.trim().isEmpty()) {
            return "firstName";
        }
        String field = sortField.trim();
        Set<String> allowedFields = Set.of("id", "email", "firstName", "lastName", "roles");
        if (allowedFields.contains(field)) {
            return field;
        }
        return "firstName"; // fallback
    }

    public List<Role> listRoles() {
        return (List<Role>) roleRepo.findAll();
    }

    public User save(User user) {
        boolean isUpdatingUser = (user.getId() != null);

        if (isUpdatingUser) {
            User existingUser = userRepo.findById(user.getId()).orElseThrow();
            if (user.getPassword().isEmpty()) {
                user.setPassword(existingUser.getPassword());
            } else {
                encodePassword(user);
            }
        } else {
            encodePassword(user);
        }

        return userRepo.save(user);
    }

    public User updateAccount(User userInForm) {
        User userInDB = userRepo.findById(userInForm.getId()).orElseThrow();

        if (!userInForm.getPassword().isEmpty()) {
            userInDB.setPassword(userInForm.getPassword());
            encodePassword(userInDB);
        }

        if (userInForm.getPhotos() != null) {
            userInDB.setPhotos(userInForm.getPhotos());
        }

        userInDB.setFirstName(userInForm.getFirstName());
        userInDB.setLastName(userInForm.getLastName());

        return userRepo.save(userInDB);
    }

    private void encodePassword(User user) {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
    }

    public boolean isEmailUnique(Integer id, String email) {
        User userByEmail = userRepo.getUserByEmail(email);
        if (userByEmail == null) return true;

        if (id == null) {
            return userByEmail == null;
        } else {
            return userByEmail.getId().equals(id);
        }
    }

    public User get(Integer id) throws UserNotFoundException {
        return userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Could not find any user with ID " + id));
    }

    public void delete(Integer id) throws UserNotFoundException {
        if (!userRepo.existsById(id)) {
            throw new UserNotFoundException("Could not find any user with ID " + id);
        }
        userRepo.deleteById(id);
    }

    public void updateUserEnabledStatus(Integer id, boolean enabled) {
        userRepo.updateEnabledStatus(id, enabled);
    }
}