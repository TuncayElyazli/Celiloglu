package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.entity.CarBrand;
import com.celiloglu.backend.entity.Category;
import com.celiloglu.backend.entity.Product;
import com.celiloglu.backend.repository.ProductRepository;
import com.celiloglu.backend.service.ProductExcelExportService;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductExcelExportServiceImpl implements ProductExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(ProductExcelExportServiceImpl.class);

    private static final String[] HEADERS = {
            "ID",
            "Məhsul adı",
            "Məhsul kodu",
            "OEM kodu",
            "Kateqoriya",
            "Avtomobil markası",
            "İl",
            "Mühərrik",
            "Stok",
            "Qiymət (AZN)",
            "Vəziyyət"
    };

    private final ProductRepository productRepository;

    public ProductExcelExportServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public byte[] exportProductsToExcel() {
        List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Məhsullar");
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

            // Mətn stili (sol tərəfə hizalanmış)
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setFont(regularFont);
            textStyle.setDataFormat(dataFormat.getFormat("@"));
            textStyle.setAlignment(HorizontalAlignment.LEFT);
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(textStyle);

            // Mətn stili (mərkəzə hizalanmış - kod, oem, il, mühərrik, vəziyyət)
            CellStyle centerTextStyle = workbook.createCellStyle();
            centerTextStyle.setFont(regularFont);
            centerTextStyle.setDataFormat(dataFormat.getFormat("@"));
            centerTextStyle.setAlignment(HorizontalAlignment.CENTER);
            centerTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(centerTextStyle);

            // ID stili (ədədi tam format)
            CellStyle idStyle = workbook.createCellStyle();
            idStyle.setFont(regularFont);
            idStyle.setDataFormat(dataFormat.getFormat("0"));
            idStyle.setAlignment(HorizontalAlignment.CENTER);
            idStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(idStyle);

            // Stok stili (ədədi tam ədəd formatı)
            CellStyle integerStyle = workbook.createCellStyle();
            integerStyle.setFont(regularFont);
            integerStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            integerStyle.setAlignment(HorizontalAlignment.RIGHT);
            integerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(integerStyle);

            // Qiymət stili (2 onluq kəsr: 0.00)
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
            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(20);

                // 0: ID
                Cell cellId = row.createCell(0, CellType.NUMERIC);
                cellId.setCellStyle(idStyle);
                if (product.getId() != null) {
                    cellId.setCellValue(product.getId());
                }

                // 1: Məhsul adı
                Cell cellName = row.createCell(1, CellType.STRING);
                cellName.setCellStyle(textStyle);
                cellName.setCellValue(product.getName() != null ? product.getName() : "");

                // 2: Məhsul kodu (mətn formatında, vahid böyük hərf standartı, öndəki sıfırlar qorunur)
                Cell cellCode = row.createCell(2, CellType.STRING);
                cellCode.setCellStyle(centerTextStyle);
                String codeStr = ProductCodeUtils.normalizeCode(product.getCode());
                cellCode.setCellValue(codeStr != null ? codeStr : "");

                // 3: OEM kodu (mətn formatında, öndəki sıfırlar qorunur)
                Cell cellOem = row.createCell(3, CellType.STRING);
                cellOem.setCellStyle(centerTextStyle);
                cellOem.setCellValue(product.getOemCode() != null ? product.getOemCode() : "");

                // 4: Kateqoriya
                Cell cellCategory = row.createCell(4, CellType.STRING);
                cellCategory.setCellStyle(textStyle);
                Category category = product.getCategory();
                String categoryName = (category != null && category.getName() != null) ? category.getName() : "";
                cellCategory.setCellValue(categoryName);

                // 5: Avtomobil markası
                Cell cellBrand = row.createCell(5, CellType.STRING);
                cellBrand.setCellStyle(textStyle);
                CarBrand brand = product.getCarBrand();
                String brandName = (brand != null && brand.getName() != null) ? brand.getName() : "";
                cellBrand.setCellValue(brandName);

                // 6: İl
                Cell cellYear = row.createCell(6, CellType.STRING);
                cellYear.setCellStyle(centerTextStyle);
                cellYear.setCellValue(product.getYearRange() != null ? product.getYearRange() : "");

                // 7: Mühərrik
                Cell cellEngine = row.createCell(7, CellType.STRING);
                cellEngine.setCellStyle(centerTextStyle);
                cellEngine.setCellValue(product.getEngine() != null ? product.getEngine() : "");

                // 8: Stok (ədədi tam format, 0 daxil olmaqla)
                Cell cellStock = row.createCell(8, CellType.NUMERIC);
                cellStock.setCellStyle(integerStyle);
                Integer stock = product.getStockQuantity();
                cellStock.setCellValue(stock != null ? stock : 0);

                // 9: Qiymət (AZN) (ədədi format, 2 onluq kəsr)
                Cell cellPrice = row.createCell(9, CellType.NUMERIC);
                cellPrice.setCellStyle(decimalStyle);
                BigDecimal price = product.getPrice();
                cellPrice.setCellValue(price != null ? price.doubleValue() : 0.0);

                // 10: Vəziyyət
                Cell cellStatus = row.createCell(10, CellType.STRING);
                cellStatus.setCellStyle(centerTextStyle);
                boolean isActive = Boolean.TRUE.equals(product.getIsActive());
                cellStatus.setCellValue(isActive ? "Aktiv" : "Deaktiv");
            }

            // Başlıq sətrini dondurmaq (freeze pane: 1-ci sətir dondurulur)
            sheet.createFreezePane(0, 1);

            // Başlıq sətrinə avto-filtr əlavə etmək
            int lastRowIndex = Math.max(0, products.size());
            sheet.setAutoFilter(new CellRangeAddress(0, lastRowIndex, 0, HEADERS.length - 1));

            // Sütun eni nizamlama (auto-fit və uyğun genişlik)
            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
                int currentWidth = sheet.getColumnWidth(col);
                int minWidth = switch (col) {
                    case 0 -> 2400;  // ID
                    case 1 -> 6500;  // Məhsul adı
                    case 2 -> 4500;  // Məhsul kodu
                    case 3 -> 4500;  // OEM kodu
                    case 4 -> 4500;  // Kateqoriya
                    case 5 -> 5000;  // Avtomobil markası
                    case 6 -> 3200;  // İl
                    case 7 -> 3200;  // Mühərrik
                    case 8 -> 3000;  // Stok
                    case 9 -> 4000;  // Qiymət (AZN)
                    case 10 -> 3500; // Vəziyyət
                    default -> 3500;
                };
                int targetWidth = Math.max(currentWidth + 1200, minWidth);
                sheet.setColumnWidth(col, Math.min(targetWidth, 18000));
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Excel faylı formalaşdırılarkən xəta baş verdi", e);
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
