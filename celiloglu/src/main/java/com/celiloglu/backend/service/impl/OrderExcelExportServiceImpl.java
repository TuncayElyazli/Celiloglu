package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.entity.Order;
import com.celiloglu.backend.entity.OrderItem;
import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.enums.OrderStatus;
import com.celiloglu.backend.repository.OrderRepository;
import com.celiloglu.backend.service.OrderExcelExportService;
import com.celiloglu.backend.service.PaymentService;
import com.celiloglu.backend.util.ProductCodeUtils;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderExcelExportServiceImpl implements OrderExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(OrderExcelExportServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String[] ORDER_HEADERS_FINANCIAL = {
            "Sifariş nömrəsi",
            "Tarix",
            "Müştəri",
            "Şirkət",
            "Ümumi məhsul miqdarı",
            "Sifariş statusu",
            "Ümumi məbləğ (AZN)",
            "Ödənilən məbləğ (AZN)",
            "Qalıq borc (AZN)",
            "Ödəniş statusu",
            "Telefon",
            "Çatdırılma ünvanı",
            "İzləmə nömrəsi"
    };

    private static final String[] ORDER_HEADERS_LOGISTICS = {
            "Sifariş nömrəsi",
            "Tarix",
            "Müştəri",
            "Şirkət",
            "Ümumi məhsul miqdarı",
            "Sifariş statusu",
            "Telefon",
            "Çatdırılma ünvanı",
            "İzləmə nömrəsi"
    };

    private static final String[] ITEM_HEADERS_FINANCIAL = {
            "Sifariş nömrəsi",
            "Məhsul adı",
            "Məhsul kodu",
            "Sifariş edilən miqdar",
            "Vahid qiymət (AZN)",
            "Sətir məbləği (AZN)"
    };

    private static final String[] ITEM_HEADERS_LOGISTICS = {
            "Sifariş nömrəsi",
            "Məhsul adı",
            "Məhsul kodu",
            "Sifariş edilən miqdar"
    };

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @Autowired
    public OrderExcelExportServiceImpl(OrderRepository orderRepository, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    @Override
    public byte[] exportOrdersToExcel(boolean includeFinancials) {
        Sort sort = Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("id"));
        List<Order> orders = orderRepository.findAllBy(sort);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

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

            // Sol tərəfə hizalanmış mətn stili (Müştəri, Şirkət, Ünvan, Məhsul adı)
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setFont(regularFont);
            textStyle.setDataFormat(dataFormat.getFormat("@"));
            textStyle.setAlignment(HorizontalAlignment.LEFT);
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(textStyle);

            // Mərkəzə hizalanmış mətn stili (Sifariş №, Tarix, Kod, Miqdar, Status, Telefon, İzləmə №)
            CellStyle centerTextStyle = workbook.createCellStyle();
            centerTextStyle.setFont(regularFont);
            centerTextStyle.setDataFormat(dataFormat.getFormat("@"));
            centerTextStyle.setAlignment(HorizontalAlignment.CENTER);
            centerTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(centerTextStyle);

            // Ədədi tam miqdar stili
            CellStyle integerStyle = workbook.createCellStyle();
            integerStyle.setFont(regularFont);
            integerStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            integerStyle.setAlignment(HorizontalAlignment.RIGHT);
            integerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(integerStyle);

            // Məbləğ stili (2 onluq kəsr: #,##0.00, sağa hizalanmış)
            CellStyle decimalStyle = workbook.createCellStyle();
            decimalStyle.setFont(regularFont);
            decimalStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
            decimalStyle.setAlignment(HorizontalAlignment.RIGHT);
            decimalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(decimalStyle);

            // ==========================================
            // VƏRƏQ 1: "Sifarişlər"
            // ==========================================
            Sheet sheetOrders = workbook.createSheet("Sifarişlər");
            String[] orderHeaders = includeFinancials ? ORDER_HEADERS_FINANCIAL : ORDER_HEADERS_LOGISTICS;

            Row orderHeaderRow = sheetOrders.createRow(0);
            orderHeaderRow.setHeightInPoints(24);
            for (int i = 0; i < orderHeaders.length; i++) {
                Cell cell = orderHeaderRow.createCell(i, CellType.STRING);
                cell.setCellValue(orderHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int orderRowIndex = 1;
            for (Order order : orders) {
                Row row = sheetOrders.createRow(orderRowIndex++);
                row.setHeightInPoints(20);

                int col = 0;

                // 0: Sifariş nömrəsi
                Cell cellOrderNumber = row.createCell(col++, CellType.STRING);
                cellOrderNumber.setCellStyle(centerTextStyle);
                String orderNumber = (order.getOrderNumber() != null && !order.getOrderNumber().isBlank())
                        ? order.getOrderNumber()
                        : "ORD-" + order.getId();
                cellOrderNumber.setCellValue(orderNumber);

                // 1: Tarix
                Cell cellDate = row.createCell(col++, CellType.STRING);
                cellDate.setCellStyle(centerTextStyle);
                String dateStr = (order.getOrderDate() != null)
                        ? order.getOrderDate().format(DATE_TIME_FORMATTER)
                        : "-";
                cellDate.setCellValue(dateStr);

                // 2: Müştəri
                Cell cellCustomer = row.createCell(col++, CellType.STRING);
                cellCustomer.setCellStyle(textStyle);
                String customerName = "-";
                String companyName = "-";
                String phone = "-";
                if (order.getUser() != null) {
                    User u = order.getUser();
                    String firstName = u.getFirstName() != null ? u.getFirstName().trim() : "";
                    String lastName = u.getLastName() != null ? u.getLastName().trim() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    if (!fullName.isEmpty()) {
                        customerName = fullName;
                    } else if (u.getUsername() != null && !u.getUsername().trim().isEmpty()) {
                        customerName = u.getUsername().trim();
                    }
                    if (u.getCompanyName() != null && !u.getCompanyName().trim().isEmpty()) {
                        companyName = u.getCompanyName().trim();
                    }
                    if (u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
                        phone = u.getPhone().trim();
                    }
                }
                cellCustomer.setCellValue(customerName);

                // 3: Şirkət
                Cell cellCompany = row.createCell(col++, CellType.STRING);
                cellCompany.setCellStyle(textStyle);
                cellCompany.setCellValue(companyName);

                // 4: Ümumi məhsul miqdarı
                long totalItems = 0;
                BigDecimal calculatedTotal = BigDecimal.ZERO;
                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        if (item != null) {
                            int qty = (item.getQuantity() != null && item.getQuantity() > 0) ? item.getQuantity() : 0;
                            totalItems += qty;
                            BigDecimal price = item.getPriceAtPurchase() != null ? item.getPriceAtPurchase() : BigDecimal.ZERO;
                            calculatedTotal = calculatedTotal.add(price.multiply(BigDecimal.valueOf(qty)));
                        }
                    }
                }
                Cell cellTotalItems = row.createCell(col++, CellType.NUMERIC);
                cellTotalItems.setCellStyle(integerStyle);
                cellTotalItems.setCellValue(totalItems);

                // 5: Sifariş statusu
                Cell cellStatus = row.createCell(col++, CellType.STRING);
                cellStatus.setCellStyle(centerTextStyle);
                String statusStr = (order.getStatus() != null)
                        ? (order.getStatus().getDisplayName() != null ? order.getStatus().getDisplayName() : order.getStatus().name())
                        : "-";
                cellStatus.setCellValue(statusStr);

                // Maliyyə sütunları (Yalnız ADMIN, SUPER_ADMIN və CASHIER üçün)
                if (includeFinancials) {
                    boolean isCancelled = order.getStatus() == OrderStatus.LƏĞV_EDİLDİ;
                    BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : calculatedTotal;
                    BigDecimal paidAmount = (order.getId() != null)
                            ? paymentService.getPaidAmountByOrderId(order.getId())
                            : BigDecimal.ZERO;
                    if (paidAmount == null) {
                        paidAmount = BigDecimal.ZERO;
                    }

                    BigDecimal remainingAmount;
                    String paymentStatusStr;
                    if (isCancelled) {
                        remainingAmount = BigDecimal.ZERO;
                        paymentStatusStr = "Ödəniş tələb olunmur";
                    } else {
                        BigDecimal remaining = totalAmount.subtract(paidAmount);
                        remainingAmount = remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
                        paymentStatusStr = (order.getPaymentStatus() != null)
                                ? (order.getPaymentStatus().getDisplayName() != null ? order.getPaymentStatus().getDisplayName() : order.getPaymentStatus().name())
                                : "-";
                    }

                    // 6: Ümumi məbləğ
                    Cell cellTotalAmount = row.createCell(col++, CellType.NUMERIC);
                    cellTotalAmount.setCellStyle(decimalStyle);
                    cellTotalAmount.setCellValue(totalAmount.doubleValue());

                    // 7: Ödənilən məbləğ
                    Cell cellPaidAmount = row.createCell(col++, CellType.NUMERIC);
                    cellPaidAmount.setCellStyle(decimalStyle);
                    cellPaidAmount.setCellValue(paidAmount.doubleValue());

                    // 8: Qalıq borc
                    Cell cellRemainingAmount = row.createCell(col++, CellType.NUMERIC);
                    cellRemainingAmount.setCellStyle(decimalStyle);
                    cellRemainingAmount.setCellValue(remainingAmount.doubleValue());

                    // 9: Ödəniş statusu
                    Cell cellPaymentStatus = row.createCell(col++, CellType.STRING);
                    cellPaymentStatus.setCellStyle(centerTextStyle);
                    cellPaymentStatus.setCellValue(paymentStatusStr);
                }

                // Telefon
                Cell cellPhone = row.createCell(col++, CellType.STRING);
                cellPhone.setCellStyle(centerTextStyle);
                cellPhone.setCellValue(phone);

                // Çatdırılma ünvanı
                Cell cellAddress = row.createCell(col++, CellType.STRING);
                cellAddress.setCellStyle(textStyle);
                String address = (order.getShippingAddress() != null && !order.getShippingAddress().isBlank())
                        ? order.getShippingAddress().trim()
                        : "-";
                cellAddress.setCellValue(address);

                // İzləmə nömrəsi
                Cell cellTracking = row.createCell(col++, CellType.STRING);
                cellTracking.setCellStyle(centerTextStyle);
                String tracking = (order.getTrackingNumber() != null && !order.getTrackingNumber().isBlank())
                        ? order.getTrackingNumber().trim()
                        : "-";
                cellTracking.setCellValue(tracking);
            }

            sheetOrders.createFreezePane(0, 1);
            sheetOrders.setAutoFilter(new CellRangeAddress(0, Math.max(0, orders.size()), 0, orderHeaders.length - 1));

            for (int i = 0; i < orderHeaders.length; i++) {
                sheetOrders.autoSizeColumn(i);
                int currentWidth = sheetOrders.getColumnWidth(i);
                int minWidth = 4200;
                int targetWidth = Math.max(currentWidth + 1200, minWidth);
                sheetOrders.setColumnWidth(i, Math.min(targetWidth, 18000));
            }

            // ==========================================
            // VƏRƏQ 2: "Məhsullar"
            // ==========================================
            Sheet sheetItems = workbook.createSheet("Məhsullar");
            String[] itemHeaders = includeFinancials ? ITEM_HEADERS_FINANCIAL : ITEM_HEADERS_LOGISTICS;

            Row itemHeaderRow = sheetItems.createRow(0);
            itemHeaderRow.setHeightInPoints(24);
            for (int i = 0; i < itemHeaders.length; i++) {
                Cell cell = itemHeaderRow.createCell(i, CellType.STRING);
                cell.setCellValue(itemHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int itemRowIndex = 1;
            for (Order order : orders) {
                String orderNumber = (order.getOrderNumber() != null && !order.getOrderNumber().isBlank())
                        ? order.getOrderNumber()
                        : "ORD-" + order.getId();

                if (order.getItems() == null || order.getItems().isEmpty()) {
                    continue;
                }

                for (OrderItem item : order.getItems()) {
                    if (item == null) {
                        continue;
                    }
                    Row row = sheetItems.createRow(itemRowIndex++);
                    row.setHeightInPoints(20);

                    int col = 0;

                    // 0: Sifariş nömrəsi
                    Cell cellOrderNum = row.createCell(col++, CellType.STRING);
                    cellOrderNum.setCellStyle(centerTextStyle);
                    cellOrderNum.setCellValue(orderNumber);

                    // 1: Məhsul adı
                    Cell cellName = row.createCell(col++, CellType.STRING);
                    cellName.setCellStyle(textStyle);
                    String productName = (item.getProduct() != null && item.getProduct().getName() != null)
                            ? item.getProduct().getName()
                            : "-";
                    cellName.setCellValue(productName);

                    // 2: Məhsul kodu (vahid böyük hərf standartı)
                    Cell cellCode = row.createCell(col++, CellType.STRING);
                    cellCode.setCellStyle(centerTextStyle);
                    String productCode = (item.getProduct() != null && item.getProduct().getCode() != null)
                            ? ProductCodeUtils.normalizeCode(item.getProduct().getCode())
                            : "-";
                    cellCode.setCellValue(productCode != null ? productCode : "-");

                    // 3: Sifariş edilən miqdar
                    int quantity = (item.getQuantity() != null && item.getQuantity() > 0) ? item.getQuantity() : 0;
                    Cell cellQty = row.createCell(col++, CellType.NUMERIC);
                    cellQty.setCellStyle(integerStyle);
                    cellQty.setCellValue(quantity);

                    // Maliyyə sütunları (Yalnız ADMIN, SUPER_ADMIN və CASHIER üçün)
                    if (includeFinancials) {
                        BigDecimal unitPrice = item.getPriceAtPurchase() != null ? item.getPriceAtPurchase() : BigDecimal.ZERO;
                        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

                        // 4: Vahid qiymət
                        Cell cellPrice = row.createCell(col++, CellType.NUMERIC);
                        cellPrice.setCellStyle(decimalStyle);
                        cellPrice.setCellValue(unitPrice.doubleValue());

                        // 5: Sətir məbləği
                        Cell cellSubtotal = row.createCell(col++, CellType.NUMERIC);
                        cellSubtotal.setCellStyle(decimalStyle);
                        cellSubtotal.setCellValue(subtotal.doubleValue());
                    }
                }
            }

            sheetItems.createFreezePane(0, 1);
            sheetItems.setAutoFilter(new CellRangeAddress(0, Math.max(0, itemRowIndex - 1), 0, itemHeaders.length - 1));

            for (int i = 0; i < itemHeaders.length; i++) {
                sheetItems.autoSizeColumn(i);
                int currentWidth = sheetItems.getColumnWidth(i);
                int minWidth = 4500;
                int targetWidth = Math.max(currentWidth + 1200, minWidth);
                sheetItems.setColumnWidth(i, Math.min(targetWidth, 18000));
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Sifarişlər Excel faylı formalaşdırılarkən xəta baş verdi", e);
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
