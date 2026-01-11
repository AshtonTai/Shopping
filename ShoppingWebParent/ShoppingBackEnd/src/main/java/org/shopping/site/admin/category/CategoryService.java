package org.shopping.site.admin.category;

import jakarta.transaction.Transactional;
import org.shopping.entity.Category;
import org.shopping.exeption.CategoryNotFoundException;
import org.shopping.site.admin.category.sort.CategorySortingStrategy;
import org.shopping.site.admin.category.sort.CategorySortingStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Transactional
public class CategoryService {
    public static final int ROOT_CATEGORIES_PER_PAGE = 4;

    @Autowired
    private CategoryRepository repo;

    @Autowired
    private CategorySortingStrategyFactory sortingStrategyFactory; // ← NEW

    public List<Category> listByPage(CategoryPageInfo pageInfo, int pageNum, String sortDir, String keyword) {
        // ← Use strategy instead of inline if/else
        CategorySortingStrategy strategy = sortingStrategyFactory.getStrategy(sortDir);
        Pageable pageable = PageRequest.of(pageNum - 1, ROOT_CATEGORIES_PER_PAGE, strategy.getSortForRootCategories());

//    public List<Category> listByPage(CategoryPageInfo pageInfo, int pageNum, String sortDir,
//                                     String keyword) {
//        Sort sort = Sort.by("name");
//
//        if (sortDir.equals("asc")) {
//            sort = sort.ascending();
//        } else if (sortDir.equals("desc")) {
//            sort = sort.descending();
//        }
//        Pageable pageable = PageRequest.of(pageNum - 1, ROOT_CATEGORIES_PER_PAGE, sort);

        Page<Category> pageCategories;
        if (keyword != null && !keyword.isEmpty()) {
            pageCategories = repo.search(keyword, pageable);
        } else {
            pageCategories = repo.findRootCategories(pageable);
        }

        List<Category> rootCategories = pageCategories.getContent();
        pageInfo.setTotalElements(pageCategories.getTotalElements());
        pageInfo.setTotalPages(pageCategories.getTotalPages());

        if (keyword != null && !keyword.isEmpty()) {
            List<Category> searchResult = pageCategories.getContent();
            for (Category category : searchResult) {
                category.setHasChildren(category.getChildren().size() > 0);
            }
            return searchResult;
        } else {
            return listHierarchicalCategories(rootCategories, strategy.getComparatorForChildren());
//            return listHierarchicalCategories(rootCategories, sortDir);
        }
    }

    private List<Category> listHierarchicalCategories(List<Category> rootCategories, Comparator<Category> childComparator) {
        List<Category> hierarchicalCategories = new ArrayList<>();

        for (Category rootCategory : rootCategories) {
            hierarchicalCategories.add(Category.copyFull(rootCategory));

            Set<Category> children = sortSubCategories(rootCategory.getChildren(), childComparator);

            for (Category subCategory : children) {
                String name = "--" + subCategory.getName();
                hierarchicalCategories.add(Category.copyFull(subCategory, name));
                listSubHierarchicalCategories(hierarchicalCategories, subCategory, 1, childComparator);
            }
        }

        return hierarchicalCategories;
    }

//    private List<Category> listHierarchicalCategories(List<Category> rootCategories, String sortDir) {
//        List<Category> hierarchicalCategories = new ArrayList<>();
//
//        for (Category rootCategory : rootCategories) {
//            hierarchicalCategories.add(Category.copyFull(rootCategory));
//
//            Set<Category> children = sortSubCategories(rootCategory.getChildren(), sortDir);
//
//            for (Category subCategory : children) {
//                String name = "--" + subCategory.getName();
//                hierarchicalCategories.add(Category.copyFull(subCategory, name));
//
//                listSubHierarchicalCategories(hierarchicalCategories, subCategory, 1, sortDir);
//            }
//        }
//
//        return hierarchicalCategories;
//    }

    private void listSubHierarchicalCategories(List<Category> hierarchicalCategories,
                                               Category parent, int subLevel, Comparator<Category> childComparator) {
        Set<Category> children = sortSubCategories(parent.getChildren(), childComparator);
        int newSubLevel = subLevel + 1;

        for (Category subCategory : children) {
            String name = "--".repeat(newSubLevel) + subCategory.getName(); // cleaner indentation
            hierarchicalCategories.add(Category.copyFull(subCategory, name));
            listSubHierarchicalCategories(hierarchicalCategories, subCategory, newSubLevel, childComparator);
        }
    }

//    private void listSubHierarchicalCategories(List<Category> hierarchicalCategories,
//                                               Category parent, int subLevel, String sortDir) {
//        Set<Category> children = sortSubCategories(parent.getChildren(), sortDir);
//        int newSubLevel = subLevel + 1;
//
//        for (Category subCategory : children) {
//            String name = "";
//            for (int i = 0; i < newSubLevel; i++) {
//                name += "--";
//            }
//            name += subCategory.getName();
//
//            hierarchicalCategories.add(Category.copyFull(subCategory, name));
//
//            listSubHierarchicalCategories(hierarchicalCategories, subCategory, newSubLevel, sortDir);
//        }
//
//    }

    public Category save(Category category) {
        Category parent = category.getParent();
        if (parent != null) {
            String allParentIds = parent.getId() == null ? "-" : parent.getAllParentIDs();
            allParentIds += String.valueOf(parent.getId()) + "-";
            category.setAllParentIDs(allParentIds);
        }
        return repo.save(category);
    }

    private String indent(int level) {
        return "--".repeat(level);
    }

    public List<Category> listCategoriesUsedInForm() {
        // ← Always sort ascending for forms — use default strategy
        CategorySortingStrategy formStrategy = sortingStrategyFactory.getStrategy("asc");
        Iterable<Category> categoriesInDB = repo.findRootCategories(formStrategy.getSortForRootCategories());

        List<Category> categoriesUsedInForm = new ArrayList<>();
        for (Category category : categoriesInDB) {
            categoriesUsedInForm.add(Category.copyIdAndName(category));

            Set<Category> children = sortSubCategories(category.getChildren(), formStrategy.getComparatorForChildren());

            for (Category subCategory : children) {
                String name = indent(1) + subCategory.getName();
                categoriesUsedInForm.add(Category.copyIdAndName(subCategory.getId(), name));
                listSubCategoriesUsedInForm(categoriesUsedInForm, subCategory, 1, formStrategy.getComparatorForChildren());
            }
        }

        return categoriesUsedInForm;
    }


    private void listSubCategoriesUsedInForm(List<Category> categoriesUsedInForm,
                                             Category parent, int subLevel, Comparator<Category> childComparator) {
        int newSubLevel = subLevel + 1;
        Set<Category> children = sortSubCategories(parent.getChildren(), childComparator);

        for (Category subCategory : children) {
            String name = indent(newSubLevel) + subCategory.getName();
            categoriesUsedInForm.add(Category.copyIdAndName(subCategory.getId(), name));
            listSubCategoriesUsedInForm(categoriesUsedInForm, subCategory, newSubLevel, childComparator);
        }
    }

//    private void listSubCategoriesUsedInForm(List<Category> categoriesUsedInForm,
//                                             Category parent, int subLevel) {
//        int newSubLevel = subLevel + 1;
//        Set<Category> children = sortSubCategories(parent.getChildren());
//
//        for (Category subCategory : children) {
//            String name = "";
//            for (int i = 0; i < newSubLevel; i++) {
//                name += "--";
//            }
//            name += subCategory.getName();
//
//            categoriesUsedInForm.add(Category.copyIdAndName(subCategory.getId(), name));
//
//            listSubCategoriesUsedInForm(categoriesUsedInForm, subCategory, newSubLevel);
//        }
//    }

    // ⚠️ NEW HELPER: sort using Comparator (replaces old string-based method)
    private SortedSet<Category> sortSubCategories(Set<Category> children, Comparator<Category> comparator) {
        SortedSet<Category> sortedChildren = new TreeSet<>(comparator);
        sortedChildren.addAll(children);
        return sortedChildren;
    }

    public Category get(Integer id) throws CategoryNotFoundException {
        try {
            return repo.findById(id).get();
        } catch (NoSuchElementException ex) {
            throw new CategoryNotFoundException("Could not find any category with ID " + id);
        }
    }

    public String checkUnique(Integer id, String name, String alias) {
        boolean isCreatingNew = (id == null || id == 0);

        Category categoryByName = repo.findByName(name);

        if (isCreatingNew) {
            if (categoryByName != null) {
                return "DuplicateName";
            } else {
                Category categoryByAlias = repo.findByAlias(alias);
                if (categoryByAlias != null) {
                    return "DuplicateAlias";
                }
            }
        } else {
            if (categoryByName != null && categoryByName.getId() != id) {
                return "DuplicateName";
            }

            Category categoryByAlias = repo.findByAlias(alias);
            if (categoryByAlias != null && categoryByAlias.getId() != id) {
                return "DuplicateAlias";
            }

        }

        return "OK";
    }

//    private SortedSet<Category> sortSubCategories(Set<Category> children) {
//        return sortSubCategories(children, "asc");
//    }

//    private SortedSet<Category> sortSubCategories(Set<Category> children, String sortDir) {
//        SortedSet<Category> sortedChildren = new TreeSet<>(new Comparator<Category>() {
//            @Override
//            public int compare(Category cat1, Category cat2) {
//                if (sortDir.equals("asc")) {
//                    return cat1.getName().compareTo(cat2.getName());
//                } else {
//                    return cat2.getName().compareTo(cat1.getName());
//                }
//            }
//        });
//
//        sortedChildren.addAll(children);
//
//        return sortedChildren;
//    }

    public void updateCategoryEnabledStatus(Integer id, boolean enabled) {
        repo.updateEnabledStatus(id, enabled);
    }

    public void delete(Integer id) throws CategoryNotFoundException {
        Long countById = repo.countById(id);
        if (countById == null || countById == 0) {
            throw new CategoryNotFoundException("Could not find any category with ID " + id);
        }

        repo.deleteById(id);
    }
}
