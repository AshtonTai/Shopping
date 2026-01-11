package org.shopping.site.admin.export;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.*;
import org.shopping.entity.User;

import java.util.List;


public class UserExcelExporter extends AbstractExporter<User> {

    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private ServletOutputStream outputStream;

    @Override
    protected String getContentType() {
        return "application/octet-stream";
    }

    @Override
    protected String getFileExtension() {
        return ".xlsx";
    }

    @Override
    protected String getFileNamePrefix() {
        return "users_";
    }

    @Override
    protected void beginDocument(HttpServletResponse response) throws Exception {
        workbook = new XSSFWorkbook();
        outputStream = response.getOutputStream();
    }

    @Override
    protected void writeHeader(HttpServletResponse response) throws Exception {
        sheet = workbook.createSheet("Users");
        XSSFRow row = sheet.createRow(0);

        CellStyle style = createHeaderCellStyle();
        createCell(row, 0, "User Id", style);
        createCell(row, 1, "E-mail", style);
        createCell(row, 2, "First Name", style);
        createCell(row, 3, "Last Name", style);
        createCell(row, 4, "Roles", style);
        createCell(row, 5, "Enabled", style);
    }

    @Override
    protected void writeDataRows(List<User> userList, HttpServletResponse response) throws Exception {
        int rowIndex = 1;
        CellStyle style = createDataCellStyle();

        for (User user : userList) {
            XSSFRow row = sheet.createRow(rowIndex++);
            int col = 0;
            createCell(row, col++, user.getId(), style);
            createCell(row, col++, user.getEmail(), style);
            createCell(row, col++, user.getFirstName(), style);
            createCell(row, col++, user.getLastName(), style);
            createCell(row, col++, user.getRoles().toString(), style);
            createCell(row, col++, user.isEnabled(), style);
        }
    }

    @Override
    protected void finishDocument(HttpServletResponse response) throws Exception {
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }

    // Helper methods (keep them private)
    private CellStyle createHeaderCellStyle() {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(16);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataCellStyle() {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(14);
        style.setFont(font);
        return style;
    }

    private void createCell(XSSFRow row, int columnIndex, Object value, CellStyle style) {
        XSSFCell cell = row.createCell(columnIndex);
        sheet.autoSizeColumn(columnIndex);
        if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue((String) value);
        }
        cell.setCellStyle(style);
    }
}