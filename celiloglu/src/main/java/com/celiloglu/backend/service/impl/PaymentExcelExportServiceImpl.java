package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.entity.Payment;
import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.enums.PaymentStatus;
import com.celiloglu.backend.repository.PaymentRepository;
import com.celiloglu.backend.service.PaymentExcelExportService;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PaymentExcelExportServiceImpl implements PaymentExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(PaymentExcelExportServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String[] HEADERS = {
            "Qəbz nömrəsi",
            "Sifariş nömrəsi",
            "Müştəri",
            "Şirkət",
            "Məbləğ (AZN)",
            "Ödəniş üsulu",
            "Tarix",
            "Qeydiyyatçı",
            "Qəbz statusu",
            "Qeyd"
    };

    private final PaymentRepository paymentRepository;

    public PaymentExcelExportServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public byte[] exportPaymentsToExcel(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Başlanğıc tarixi bitiş tarixindən sonra ola bilməz.");
        }

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;
        Sort sort = Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id"));

        List<Payment> payments;
        if (startDateTime != null && endDateTime != null) {
            payments = paymentRepository.findByPaymentDateGreaterThanEqualAndPaymentDateLessThan(startDateTime, endDateTime, sort);
        } else if (startDateTime != null) {
            payments = paymentRepository.findByPaymentDateGreaterThanEqual(startDateTime, sort);
        } else if (endDateTime != null) {
            payments = paymentRepository.findByPaymentDateLessThan(endDateTime, sort);
        } else {
            payments = paymentRepository.findAllBy(sort);
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Ödənişlər");
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

            // Başlıq stili
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(headerStyle);

            // Sol tərəfə hizalanmış mətn stili (Müştəri, Şirkət, Ödəniş üsulu, Qeyd)
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setFont(regularFont);
            textStyle.setDataFormat(dataFormat.getFormat("@"));
            textStyle.setAlignment(HorizontalAlignment.LEFT);
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(textStyle);

            // Mərkəzə hizalanmış mətn stili (Qəbz №, Sifariş №, Tarix, Qeydiyyatçı, Qəbz statusu)
            CellStyle centerTextStyle = workbook.createCellStyle();
            centerTextStyle.setFont(regularFont);
            centerTextStyle.setDataFormat(dataFormat.getFormat("@"));
            centerTextStyle.setAlignment(HorizontalAlignment.CENTER);
            centerTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(centerTextStyle);

            // Məbləğ stili (2 onluq kəsr: #,##0.00, sağa hizalanmış)
            CellStyle decimalStyle = workbook.createCellStyle();
            decimalStyle.setFont(regularFont);
            decimalStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
            decimalStyle.setAlignment(HorizontalAlignment.RIGHT);
            decimalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(decimalStyle);

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
            for (Payment payment : payments) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(20);

                // 0: Qəbz nömrəsi
                Cell cellReceipt = row.createCell(0, CellType.STRING);
                cellReceipt.setCellStyle(centerTextStyle);
                cellReceipt.setCellValue(payment.getReceiptNumber() != null ? payment.getReceiptNumber() : "-");

                // 1: Sifariş nömrəsi
                Cell cellOrder = row.createCell(1, CellType.STRING);
                cellOrder.setCellStyle(centerTextStyle);
                String orderNumber = (payment.getOrder() != null && payment.getOrder().getOrderNumber() != null)
                        ? payment.getOrder().getOrderNumber()
                        : "-";
                cellOrder.setCellValue(orderNumber);

                // 2: Müştəri
                Cell cellCustomer = row.createCell(2, CellType.STRING);
                cellCustomer.setCellStyle(textStyle);
                String customerName = "-";
                if (payment.getOrder() != null && payment.getOrder().getUser() != null) {
                    User u = payment.getOrder().getUser();
                    String firstName = u.getFirstName() != null ? u.getFirstName().trim() : "";
                    String lastName = u.getLastName() != null ? u.getLastName().trim() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    if (!fullName.isEmpty()) {
                        customerName = fullName;
                    } else if (u.getUsername() != null && !u.getUsername().trim().isEmpty()) {
                        customerName = u.getUsername().trim();
                    }
                }
                cellCustomer.setCellValue(customerName);

                // 3: Şirkət
                Cell cellCompany = row.createCell(3, CellType.STRING);
                cellCompany.setCellStyle(textStyle);
                String companyName = "-";
                if (payment.getOrder() != null && payment.getOrder().getUser() != null) {
                    User u = payment.getOrder().getUser();
                    if (u.getCompanyName() != null && !u.getCompanyName().trim().isEmpty()) {
                        companyName = u.getCompanyName().trim();
                    }
                }
                if ("-".equals(companyName) && payment.getCustomerCompany() != null && !payment.getCustomerCompany().trim().isEmpty()) {
                    companyName = payment.getCustomerCompany().trim();
                }
                cellCompany.setCellValue(companyName);

                // 4: Məbləğ (AZN) - Faktiki daxil olan pul
                Cell cellAmount = row.createCell(4, CellType.NUMERIC);
                cellAmount.setCellStyle(decimalStyle);
                BigDecimal amount = payment.getAmount();
                cellAmount.setCellValue(amount != null ? amount.doubleValue() : 0.0);

                // 5: Ödəniş üsulu
                Cell cellMethod = row.createCell(5, CellType.STRING);
                cellMethod.setCellStyle(textStyle);
                String method = (payment.getPaymentMethod() != null && payment.getPaymentMethod().getDisplayName() != null)
                        ? payment.getPaymentMethod().getDisplayName()
                        : "-";
                cellMethod.setCellValue(method);

                // 6: Tarix (dd.MM.yyyy HH:mm)
                Cell cellDate = row.createCell(6, CellType.STRING);
                cellDate.setCellStyle(centerTextStyle);
                String dateStr = (payment.getPaymentDate() != null)
                        ? payment.getPaymentDate().format(DATE_TIME_FORMATTER)
                        : "-";
                cellDate.setCellValue(dateStr);

                // 7: Qeydiyyatçı
                Cell cellRecordedBy = row.createCell(7, CellType.STRING);
                cellRecordedBy.setCellStyle(centerTextStyle);
                String recordedBy = (payment.getRecordedBy() != null && !payment.getRecordedBy().trim().isEmpty())
                        ? payment.getRecordedBy().trim()
                        : "-";
                cellRecordedBy.setCellValue(recordedBy);

                // 8: Qəbz statusu (TAM_ÖDƏNİLMİŞ -> "Təsdiqlənib")
                Cell cellStatus = row.createCell(8, CellType.STRING);
                cellStatus.setCellStyle(centerTextStyle);
                String statusText = "-";
                if (payment.getStatus() != null) {
                    if (payment.getStatus() == PaymentStatus.TAM_ÖDƏNİLMİŞ) {
                        statusText = "Təsdiqlənib";
                    } else if (payment.getStatus().getDisplayName() != null) {
                        statusText = payment.getStatus().getDisplayName();
                    } else {
                        statusText = payment.getStatus().name();
                    }
                }
                cellStatus.setCellValue(statusText);

                // 9: Qeyd
                Cell cellNotes = row.createCell(9, CellType.STRING);
                cellNotes.setCellStyle(textStyle);
                cellNotes.setCellValue(payment.getNotes() != null ? payment.getNotes().trim() : "");
            }

            // Başlıq sətrini dondurmaq (freeze pane: 1-ci sətir dondurulur)
            sheet.createFreezePane(0, 1);

            // Başlıq sətrinə avto-filtr əlavə etmək
            int lastRowIndex = Math.max(0, payments.size());
            sheet.setAutoFilter(new CellRangeAddress(0, lastRowIndex, 0, HEADERS.length - 1));

            // Sütun eni nizamlama (auto-fit və uyğun genişlik)
            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
                int currentWidth = sheet.getColumnWidth(col);
                int minWidth = switch (col) {
                    case 0 -> 4500;  // Qəbz nömrəsi
                    case 1 -> 4500;  // Sifariş nömrəsi
                    case 2 -> 6000;  // Müştəri
                    case 3 -> 6000;  // Şirkət
                    case 4 -> 4000;  // Məbləğ (AZN)
                    case 5 -> 5000;  // Ödəniş üsulu
                    case 6 -> 4500;  // Tarix
                    case 7 -> 4000;  // Qeydiyyatçı
                    case 8 -> 4000;  // Qəbz statusu
                    case 9 -> 6000;  // Qeyd
                    default -> 4000;
                };
                int targetWidth = Math.max(currentWidth + 1200, minWidth);
                sheet.setColumnWidth(col, Math.min(targetWidth, 18000));
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Ödənişlər Excel faylı formalaşdırılarkən xəta baş verdi", e);
            throw new RuntimeException("Excel faylı formalaşdırıla bilmədi: " + e.getMessage(), e);
        }
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}

