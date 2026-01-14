package org.shopping.site.admin.paging;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public class PagingAndSortingHelper {

    private final String sortField;
    private final String sortDir;
    private final String keyword;

    public PagingAndSortingHelper(String sortField, String sortDir, String keyword) {
        this.sortField = sortField;
        this.sortDir = sortDir;
        this.keyword = keyword;
    }

    // Create pageable with validation
    public Pageable createPageable(int pageSize, int pageNum, List<String> allowedSortFields) {
        String validatedSortField = allowedSortFields.contains(sortField) ? sortField : allowedSortFields.get(0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageNum - 1, pageSize, Sort.by(direction, validatedSortField));
    }

    // Getters
    public String getSortField() { return sortField; }
    public String getSortDir() { return sortDir; }
    public String getKeyword() { return keyword; }
}