package org.shopping.site.admin.export;

import jakarta.servlet.http.HttpServletResponse;
import org.shopping.entity.User;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.PrintWriter;
import java.util.List;


public class UserCsvExporter extends AbstractExporter<User> {

    private ICsvBeanWriter csvWriter;
    private PrintWriter printWriter;

    // Implement abstract methods from template
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
        return "users_";
    }

    @Override
    protected void beginDocument(HttpServletResponse response) throws Exception {
        printWriter = response.getWriter();
        csvWriter = new CsvBeanWriter(printWriter, CsvPreference.STANDARD_PREFERENCE);
    }

    @Override
    protected void writeHeader(HttpServletResponse response) throws Exception {
        String[] header = {"User ID", "E-mail", "First Name", "Last Name", "Roles", "Enabled"};
        csvWriter.writeHeader(header);
    }

    @Override
    protected void writeDataRows(List<User> userList, HttpServletResponse response) throws Exception {
        String[] fieldMapping = {"id", "email", "firstName", "lastName", "roles", "enabled"};
        for (User user : userList) {
            csvWriter.write(user, fieldMapping);
        }
    }

    @Override
    protected void finishDocument(HttpServletResponse response) throws Exception {
        csvWriter.close();
    }

}