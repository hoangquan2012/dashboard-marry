package com.wedding.management.common.excel;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Small helper around Apache POI for consistent, Vietnamese-formatted workbooks.
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    public static final class Styles {
        public final CellStyle header;
        public final CellStyle money;
        public final CellStyle date;
        public final CellStyle text;
        public final CellStyle title;
        public final CellStyle bold;
        public final CellStyle boldMoney;

        Styles(Workbook wb) {
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            header = wb.createCellStyle();
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.BROWN.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setBorderBottom(BorderStyle.THIN);

            money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0 \"₫\""));

            date = wb.createCellStyle();
            date.setDataFormat(wb.createDataFormat().getFormat("dd/mm/yyyy"));

            text = wb.createCellStyle();

            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            title = wb.createCellStyle();
            title.setFont(titleFont);

            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            bold = wb.createCellStyle();
            bold.setFont(boldFont);

            boldMoney = wb.createCellStyle();
            boldMoney.setFont(boldFont);
            boldMoney.setDataFormat(wb.createDataFormat().getFormat("#,##0 \"₫\""));
        }
    }

    public static Workbook newWorkbook() {
        return new XSSFWorkbook();
    }

    public static Styles styles(Workbook wb) {
        return new Styles(wb);
    }

    public static Row header(Sheet sheet, int rowIndex, Styles styles, List<String> titles) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < titles.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(titles.get(i));
            cell.setCellStyle(styles.header);
        }
        return row;
    }

    public static void text(Row row, int col, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
    }

    public static void number(Row row, int col, Number value) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    public static void money(Row row, int col, BigDecimal value, Styles styles) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(styles.money);
    }

    public static void boldMoney(Row row, int col, BigDecimal value, Styles styles) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(styles.boldMoney);
    }

    public static void date(Row row, int col, LocalDate value, Styles styles) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(styles.date);
    }

    public static void bold(Row row, int col, String value, Styles styles) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(styles.bold);
    }

    public static void title(Sheet sheet, int rowIndex, String text, Styles styles) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(styles.title);
    }

    public static void autosize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(Math.max(width + 600, 3000), 15000));
        }
    }

    public static byte[] toBytes(Workbook wb) {
        try (wb; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---- reading -------------------------------------------------------------

    public static String readString(Row row, int col) {
        if (row == null) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> trimToNull(cell.getStringCellValue());
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == Math.rint(d)) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield trimToNull(cell.getStringCellValue());
                } catch (IllegalStateException e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> null;
        };
    }

    public static Integer readInt(Row row, int col) {
        String s = readString(row, col);
        if (s == null) return null;
        try {
            return (int) Math.round(Double.parseDouble(s.replace(",", "").replace(".", "")));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal readMoney(Row row, int col) {
        if (row == null) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(Math.round(cell.getNumericCellValue()));
        }
        String s = readString(row, col);
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9-]", "");
        if (digits.isEmpty() || "-".equals(digits)) return null;
        return new BigDecimal(digits);
    }

    public static LocalDate readDate(Row row, int col) {
        if (row == null) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String s = readString(row, col);
        if (s == null) return null;
        try {
            if (s.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                String[] p = s.split("/");
                return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
            }
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isRowEmpty(Row row, int columns) {
        if (row == null) return true;
        for (int i = 0; i < columns; i++) {
            if (readString(row, i) != null) return false;
        }
        return true;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
