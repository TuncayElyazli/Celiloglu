package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.dto.response.B2BAnalyticsSummaryDto;
import com.celiloglu.backend.dto.response.B2BCustomerAnalyticsDto;
import com.celiloglu.backend.service.AdminAnalyticsService;
import com.celiloglu.backend.service.CustomerAnalyticsExcelExportService;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerAnalyticsExcelExportServiceImpl implements CustomerAnalyticsExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(CustomerAnalyticsExcelExportServiceImpl.class);

    private static final String[] HEADERS = {
            "Müştəri adı",
            "İstifadəçi adı",
            "Şirkət",
            "Sifariş sayı",
            "Sifarişlərin ümumi məbləği (AZN)",
            "Ödənilmiş məbləğ (AZN)",
            "Qalıq borc (AZN)"
    };

    private final AdminAnalyticsService analyticsService;

    @Autowired
    public CustomerAnalyticsExcelExportServiceImpl(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    public byte[] exportCustomerAnalyticsToExcel() {
        List<B2BCustomerAnalyticsDto> customers = analyticsService.getAllCustomerAnalytics();
        B2BAnalyticsSummaryDto summary = analyticsService.getAnalyticsSummary();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Müştəri Hesablaşmaları");
            DataFormat dataFormat = workbook.createDataFormat();

            // Fontlar
            Font headerFont = workbook.createFont();
            headerFont.setFontName("Calibri");
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.BLACK.getIndex());

            Font regularFont = workbook.createFont();
            regularFont.setFontName("Calibri");
            regularFont.setFontHeightInPoints((short) 10);

            Font totalFont = workbook.createFont();
            totalFont.setFontName("Calibri");
            totalFont.setBold(true);
            totalFont.setFontHeightInPoints((short) 10);
            totalFont.setColor(IndexedColors.BLACK.getIndex());

            // Başlıq stili
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(headerStyle);

            // Mətn stili (sol tərəfə hizalanmış - Müştəri adı, Şirkət)
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setFont(regularFont);
            textStyle.setDataFormat(dataFormat.getFormat("@"));
            textStyle.setAlignment(HorizontalAlignment.LEFT);
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(textStyle);

            // İstifadəçi adı stili (mərkəzə hizalanmış)
            CellStyle centerTextStyle = workbook.createCellStyle();
            centerTextStyle.setFont(regularFont);
            centerTextStyle.setDataFormat(dataFormat.getFormat("@"));
            centerTextStyle.setAlignment(HorizontalAlignment.CENTER);
            centerTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(centerTextStyle);

            // Sifariş sayı stili (tam ədəd, sağa hizalanmış)
            CellStyle integerStyle = workbook.createCellStyle();
            integerStyle.setFont(regularFont);
            integerStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            integerStyle.setAlignment(HorizontalAlignment.RIGHT);
            integerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(integerStyle);

            // Maliyyə məbləğləri stili (2 onluq kəsr: #,##0.00, sağa hizalanmış)
            CellStyle decimalStyle = workbook.createCellStyle();
            decimalStyle.setFont(regularFont);
            decimalStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
            decimalStyle.setAlignment(HorizontalAlignment.RIGHT);
            decimalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(decimalStyle);

            // Yekun "Cəmi" sol mətn stili
            CellStyle totalLabelStyle = workbook.createCellStyle();
            totalLabelStyle.setFont(totalFont);
            totalLabelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalLabelStyle.setAlignment(HorizontalAlignment.LEFT);
            totalLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyTotalBorders(totalLabelStyle);

            // Yekun "Cəmi" mərkəz stili
            CellStyle totalCenterStyle = workbook.createCellStyle();
            totalCenterStyle.setFont(totalFont);
            totalCenterStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalCenterStyle.setAlignment(HorizontalAlignment.CENTER);
            totalCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyTotalBorders(totalCenterStyle);

            // Yekun "Cəmi" sifariş sayı stili
            CellStyle totalIntegerStyle = workbook.createCellStyle();
            totalIntegerStyle.setFont(totalFont);
            totalIntegerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalIntegerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalIntegerStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            totalIntegerStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalIntegerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyTotalBorders(totalIntegerStyle);

            // Yekun "Cəmi" maliyyə məbləğləri stili
            CellStyle totalDecimalStyle = workbook.createCellStyle();
            totalDecimalStyle.setFont(totalFont);
            totalDecimalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalDecimalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalDecimalStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
            totalDecimalStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalDecimalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyTotalBorders(totalDecimalStyle);

            // Başlıq sətri
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i, CellType.STRING);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Məlumat sətirləri
            int rowIndex = 1;
            long totalOrderCount = 0;
            for (B2BCustomerAnalyticsDto customer : customers) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(20);

                // 0: Müştəri adı
                Cell cellFullName = row.createCell(0, CellType.STRING);
                cellFullName.setCellStyle(textStyle);
                cellFullName.setCellValue(customer.getFullName() != null && !customer.getFullName().isBlank() ? customer.getFullName().trim() : "—");

                // 1: İstifadəçi adı
                Cell cellUsername = row.createCell(1, CellType.STRING);
                cellUsername.setCellStyle(centerTextStyle);
                cellUsername.setCellValue(customer.getUsername() != null && !customer.getUsername().isBlank() ? customer.getUsername().trim() : "—");

                // 2: Şirkət
                Cell cellCompany = row.createCell(2, CellType.STRING);
                cellCompany.setCellStyle(textStyle);
                String company = customer.getCompanyName() != null && !customer.getCompanyName().isBlank() && !customer.getCompanyName().equals("—")
                        ? customer.getCompanyName().trim()
                        : "—";
                cellCompany.setCellValue(company);

                // 3: Sifariş sayı
                long orderCount = customer.getOrderCount() != null ? customer.getOrderCount() : 0L;
                totalOrderCount += orderCount;
                Cell cellOrderCount = row.createCell(3, CellType.NUMERIC);
                cellOrderCount.setCellStyle(integerStyle);
                cellOrderCount.setCellValue(orderCount);

                // 4: Sifarişlərin ümumi məbləği (AZN)
                BigDecimal orderAmount = customer.getTotalOrderAmount() != null ? customer.getTotalOrderAmount() : BigDecimal.ZERO;
                Cell cellTotalOrder = row.createCell(4, CellType.NUMERIC);
                cellTotalOrder.setCellStyle(decimalStyle);
                cellTotalOrder.setCellValue(orderAmount.doubleValue());

                // 5: Ödənilmiş məbləğ (AZN)
                BigDecimal paidAmount = customer.getTotalPaidAmount() != null ? customer.getTotalPaidAmount() : BigDecimal.ZERO;
                Cell cellPaid = row.createCell(5, CellType.NUMERIC);
                cellPaid.setCellStyle(decimalStyle);
                cellPaid.setCellValue(paidAmount.doubleValue());

                // 6: Qalıq borc (AZN)
                BigDecimal debtAmount = customer.getRemainingDebt() != null ? customer.getRemainingDebt() : BigDecimal.ZERO;
                Cell cellDebt = row.createCell(6, CellType.NUMERIC);
                cellDebt.setCellStyle(decimalStyle);
                cellDebt.setCellValue(debtAmount.doubleValue());
            }

            // Yekun "Cəmi" sətri
            Row totalRow = sheet.createRow(rowIndex);
            totalRow.setHeightInPoints(22);

            BigDecimal summaryTotalOrder = summary != null && summary.getTotalOrderAmount() != null
                    ? summary.getTotalOrderAmount()
                    : BigDecimal.ZERO;
            BigDecimal summaryTotalPaid = summary != null && summary.getTotalPaidAmount() != null
                    ? summary.getTotalPaidAmount()
                    : BigDecimal.ZERO;
            BigDecimal summaryTotalDebt = summary != null && summary.getTotalRemainingDebt() != null
                    ? summary.getTotalRemainingDebt()
                    : BigDecimal.ZERO;

            // 0: Cəmi etiketi
            Cell cellTotalLabel = totalRow.createCell(0, CellType.STRING);
            cellTotalLabel.setCellStyle(totalLabelStyle);
            cellTotalLabel.setCellValue("Cəmi");

            // 1: İstifadəçi adı (boş)
            Cell cellTotalUser = totalRow.createCell(1, CellType.STRING);
            cellTotalUser.setCellStyle(totalCenterStyle);
            cellTotalUser.setCellValue("");

            // 2: Şirkət (boş)
            Cell cellTotalCompany = totalRow.createCell(2, CellType.STRING);
            cellTotalCompany.setCellStyle(totalCenterStyle);
            cellTotalCompany.setCellValue("");

            // 3: Cəmi sifariş sayı
            Cell cellTotalOrders = totalRow.createCell(3, CellType.NUMERIC);
            cellTotalOrders.setCellStyle(totalIntegerStyle);
            cellTotalOrders.setCellValue(totalOrderCount);

            // 4: Sifarişlərin ümumi məbləği cəmi (saytın ümumi göstəricisi ilə eyni)
            Cell cellSumOrder = totalRow.createCell(4, CellType.NUMERIC);
            cellSumOrder.setCellStyle(totalDecimalStyle);
            cellSumOrder.setCellValue(summaryTotalOrder.doubleValue());

            // 5: Ödənilmiş məbləğ cəmi (saytın ümumi göstəricisi ilə eyni)
            Cell cellSumPaid = totalRow.createCell(5, CellType.NUMERIC);
            cellSumPaid.setCellStyle(totalDecimalStyle);
            cellSumPaid.setCellValue(summaryTotalPaid.doubleValue());

            // 6: Qalıq borc cəmi (saytın ümumi göstəricisi ilə eyni)
            Cell cellSumDebt = totalRow.createCell(6, CellType.NUMERIC);
            cellSumDebt.setCellStyle(totalDecimalStyle);
            cellSumDebt.setCellValue(summaryTotalDebt.doubleValue());

            // Başlıq sətrini dondurmaq (freeze pane: 1-ci sətir dondurulur)
            sheet.createFreezePane(0, 1);

            // Başlıq sətrinə avto-filtr əlavə etmək (məlumat sətirlərini əhatə edir)
            int lastDataRowIndex = Math.max(0, rowIndex - 1);
            sheet.setAutoFilter(new CellRangeAddress(0, lastDataRowIndex, 0, HEADERS.length - 1));

            // Sütun eni nizamlama (auto-fit və təyin olunmuş minimum genişlik)
            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
                int currentWidth = sheet.getColumnWidth(col);
                int minWidth = switch (col) {
                    case 0 -> 6500;  // Müştəri adı
                    case 1 -> 4500;  // İstifadəçi adı
                    case 2 -> 5500;  // Şirkət
                    case 3 -> 3500;  // Sifariş sayı
                    case 4 -> 6800;  // Sifarişlərin ümumi məbləği (AZN)
                    case 5 -> 6200;  // Ödənilmiş məbləğ (AZN)
                    case 6 -> 5800;  // Qalıq borc (AZN)
                    default -> 4500;
                };
                int targetWidth = Math.max(currentWidth + 1200, minWidth);
                sheet.setColumnWidth(col, Math.min(targetWidth, 18000));
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Müştəri hesablaşmaları Excel faylı formalaşdırılarkən xəta baş verdi", e);
            throw new RuntimeException("Excel faylı formalaşdırıla bilmədi: " + e.getMessage(), e);
        }
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void applyTotalBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.DOUBLE);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}

