package org.shopping.site.admin.brand;

import org.shopping.entity.Brand;
import org.shopping.exeption.BrandNotFoundException;
import org.shopping.site.admin.paging.PagingAndSortingHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class BrandService {
    public static final int BRANDS_PER_PAGE = 10;

    @Autowired
    private BrandRepository repo;

    public List<Brand> listAll() {
        return (List<Brand>) repo.findAll();
    }

    public Page<Brand> listByPage(int pageNum, PagingAndSortingHelper helper) {
        // Only allow sorting by valid Brand fields
        List<String> allowedSortFields = List.of("id", "name");

        Pageable pageable = helper.createPageable(BRANDS_PER_PAGE, pageNum, allowedSortFields);

        String keyword = helper.getKeyword();
        if (keyword != null && !keyword.isEmpty()) {
            return repo.findAll(keyword, pageable);
        } else {
            return repo.findAll(pageable);
        }
    }

    public Brand save(Brand brand) {
        return repo.save(brand);
    }

    public Brand get(Integer id) throws BrandNotFoundException {
        try {
            return repo.findById(id).get();
        } catch (NoSuchElementException ex) {
            throw new BrandNotFoundException("Could not find any brand with ID " + id);
        }
    }

    public void delete(Integer id) throws BrandNotFoundException {
        Long countById = repo.countById(id);

        if (countById == null || countById == 0) {
            throw new BrandNotFoundException("Could not find any brand with ID " + id);
        }

        repo.deleteById(id);
    }

    public String checkUnique(Integer id, String name) {
        boolean isCreatingNew = (id == null || id == 0);
        Brand brandByName = repo.findByName(name);

        if (isCreatingNew) {
            if (brandByName != null) return "Duplicate";
        } else {
            if (brandByName != null && brandByName.getId() != id) {
                return "Duplicate";
            }
        }

        return "OK";
    }
}
