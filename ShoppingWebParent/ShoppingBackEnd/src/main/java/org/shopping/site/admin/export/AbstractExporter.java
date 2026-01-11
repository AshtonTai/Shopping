package org.shopping.site.admin.export;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

// Make it generic: <T> = entity type (User, Category, etc.)
public abstract class AbstractExporter<T> {

    // Template method: now works for ANY entity type
    public final void export(List<T> dataList, HttpServletResponse response) throws Exception {
        setResponseHeader(response);
        beginDocument(response);
        writeHeader(response);
        writeDataRows(dataList, response);
        finishDocument(response);
    }

    // Abstract methods — subclasses provide format-specific logic
    protected abstract String getContentType();
    protected abstract String getFileExtension();
    protected abstract String getFileNamePrefix();

    // Reusable helper
    protected void setResponseHeader(HttpServletResponse response) throws Exception {
        java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = dateFormat.format(new java.util.Date());
        String fileName = getFileNamePrefix() + timestamp + getFileExtension();

        response.setContentType(getContentType());
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
    }

    // Abstract steps — operate on generic T
    protected abstract void beginDocument(HttpServletResponse response) throws Exception;
    protected abstract void writeHeader(HttpServletResponse response) throws Exception;
    protected abstract void writeDataRows(List<T> dataList, HttpServletResponse response) throws Exception;
    protected abstract void finishDocument(HttpServletResponse response) throws Exception;
}