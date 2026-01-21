package org.shopping.site.admin.brand;

import org.shopping.entity.Brand;
import org.shopping.site.admin.paging.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BrandRepository extends SearchRepository<Brand, Integer> {

    Long countById(Integer id);

    Brand findByName(String name);

    @Query("SELECT b FROM Brand b WHERE b.name LIKE %:keyword%")
    Page<Brand> findAll(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT NEW Brand(b.id, b.name) FROM Brand b ORDER BY b.name ASC")
    List<Brand> findAllBrandsForDropdown(); // ← better name than findAll()
}