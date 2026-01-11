package org.shopping.site.admin.category;

import jakarta.servlet.http.HttpServletResponse;
import org.shopping.entity.Category;
import org.shopping.site.admin.export.AbstractExporter;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.PrintWriter;
import java.util.List;

public class CategoryCsvExporter extends AbstractExporter<Category> {

    private ICsvBeanWriter csvWriter;
    private PrintWriter printWriter;

    @Override
    protected String getContentType() {
        return "text/csv";
    }

    @Override
    protected String getFileExtension() {
        return ".csv";
    }

    @Override
    protected String getFileNamePrefix() {
        return "categories_";
    }

    @Override
    protected void beginDocument(HttpServletResponse response) throws Exception {
        printWriter = response.getWriter();
        csvWriter = new CsvBeanWriter(printWriter, CsvPreference.STANDARD_PREFERENCE);
    }

    @Override
    protected void writeHeader(HttpServletResponse response) throws Exception {
        String[] header = {"Category ID", "Category Name"};
        csvWriter.writeHeader(header);
    }

    @Override
    protected void writeDataRows(List<Category> listCategories, HttpServletResponse response) throws Exception {
        String[] fieldMapping = {"id", "name"};
        for (Category category : listCategories) {
            // Clean name (as in original)
            category.setName(category.getName().replace("--", "  "));
            csvWriter.write(category, fieldMapping);
        }
    }

    @Override
    protected void finishDocument(HttpServletResponse response) throws Exception {
        csvWriter.close();
    }
}