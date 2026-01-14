package org.shopping.site.admin.paging;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class PagingAndSortingArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(PagingAndSortingParam.class) != null;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer model,
            NativeWebRequest request,
            WebDataBinderFactory binderFactory) throws Exception {

        PagingAndSortingParam annotation = parameter.getParameterAnnotation(PagingAndSortingParam.class);

        String sortDir = request.getParameter("sortDir");
        String sortField = request.getParameter("sortField");
        String keyword = request.getParameter("keyword");

        // Provide defaults if missing
        if (sortField == null || sortField.isEmpty()) {
            sortField = "id"; // or a default field appropriate for the page
        }
        if (sortDir == null || sortDir.isEmpty()) {
            sortDir = "asc";
        }

        // Add sorting metadata to model (for Thymeleaf links/buttons)
        String reverseSortDir = sortDir.equals("asc") ? "desc" : "asc";
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", reverseSortDir);
        model.addAttribute("keyword", keyword);
        model.addAttribute("moduleURL", annotation.moduleURL());

        // ✅ ONLY pass the 3 string parameters — NO 'model', NO 'listName'
        return new PagingAndSortingHelper(sortField, sortDir, keyword);
    }

}