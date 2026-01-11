package org.shopping.site.admin.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.shopping.entity.User;

import java.awt.Color;
import java.util.List;


public class UserPdfExporter extends AbstractExporter<User> {

    private Document document;
    private PdfPTable table;

    @Override
    protected String getContentType() {
        return "application/pdf";
    }

    @Override
    protected String getFileExtension() {
        return ".pdf";
    }

    @Override
    protected String getFileNamePrefix() {
        return "users_";
    }

    @Override
    protected void beginDocument(HttpServletResponse response) throws Exception {
        document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        // Add title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLUE);
        Paragraph title = new Paragraph("List of User", titleFont);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(title);

        // Prepare table
        table = new PdfPTable(6);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{1.2f, 3.5f, 3.0f, 3.0f, 3.0f, 1.7f});
    }

    @Override
    protected void writeHeader(HttpServletResponse response) throws Exception {
        writeTableHeader(table);
    }

    @Override
    protected void writeDataRows(List<User> userList, HttpServletResponse response) throws Exception {
        for (User user : userList) {
            table.addCell(String.valueOf(user.getId()));
            table.addCell(user.getEmail());
            table.addCell(user.getFirstName());
            table.addCell(user.getLastName());
            table.addCell(user.getRoles().toString());
            table.addCell(String.valueOf(user.isEnabled()));
        }
    }

    @Override
    protected void finishDocument(HttpServletResponse response) throws Exception {
        document.add(table);
        document.close();
    }

    // Keep helper method
    private void writeTableHeader(PdfPTable table) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(Color.BLUE);
        cell.setPadding(5);
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);

        cell.setPhrase(new Phrase("ID", font));
        table.addCell(cell);
        cell.setPhrase(new Phrase("E-mail", font));
        table.addCell(cell);
        cell.setPhrase(new Phrase("First Name", font));
        table.addCell(cell);
        cell.setPhrase(new Phrase("Last Name", font));
        table.addCell(cell);
        cell.setPhrase(new Phrase("Roles", font));
        table.addCell(cell);
        cell.setPhrase(new Phrase("Enabled", font));
        table.addCell(cell);
    }
}