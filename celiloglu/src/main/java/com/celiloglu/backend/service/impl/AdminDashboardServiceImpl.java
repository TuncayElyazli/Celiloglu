package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.dto.response.AdminDashboardDto;
import com.celiloglu.backend.dto.response.OrderResponseDto;
import com.celiloglu.backend.dto.response.PaymentChartAxisLabelDto;
import com.celiloglu.backend.dto.response.PaymentChartDto;
import com.celiloglu.backend.dto.response.PaymentChartPointDto;
import com.celiloglu.backend.enums.OrderStatus;
import com.celiloglu.backend.enums.PaymentStatus;
import com.celiloglu.backend.repository.OrderRepository;
import com.celiloglu.backend.repository.PaymentRepository;
import com.celiloglu.backend.repository.ProductRepository;
import com.celiloglu.backend.service.AdminDashboardService;
import com.celiloglu.backend.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public AdminDashboardServiceImpl(OrderRepository orderRepository,
                                     PaymentRepository paymentRepository,
                                     ProductRepository productRepository,
                                     OrderService orderService) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "OrderRepository boş ola bilməz!");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "PaymentRepository boş ola bilməz!");
        this.productRepository = Objects.requireNonNull(productRepository, "ProductRepository boş ola bilməz!");
        this.orderService = Objects.requireNonNull(orderService, "OrderService boş ola bilməz!");
    }

    @Override
    public AdminDashboardDto getDashboardData() {
        // 1. Ümumi sifariş sayı: ləğv edilənlər daxil bütün sifarişlər
        long totalOrders = orderRepository.count();

        // 2. Gözləyən sifariş sayı: yalnız OrderStatus.GÖZLƏYİR
        long pendingOrders = orderRepository.countByStatus(OrderStatus.GÖZLƏYİR);

        // 3. Aktiv məhsul sayı: isActive=true (stokundan asılı olmayaraq)
        long activeProducts = productRepository.countByIsActiveTrue();

        // 4. Daxil olmuş ödənişlər: Payment.status=TAM_ÖDƏNİLMİŞ olan qəbzlərin amount cəmi
        BigDecimal receivedPayments = paymentRepository.sumAmountByStatus(PaymentStatus.TAM_ÖDƏNİLMİŞ);
        if (receivedPayments == null) {
            receivedPayments = BigDecimal.ZERO;
        }

        // 5. Ən son 5 sifariş: mövcud getAllOrdersForAdmin(Pageable) istifadə edilir (orderDate DESC, id DESC)
        Page<OrderResponseDto> recentOrdersPage = orderService.getAllOrdersForAdmin(
                PageRequest.of(0, 5, Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("id")))
        );
        List<OrderResponseDto> recentOrders = (recentOrdersPage != null)
                ? recentOrdersPage.getContent()
                : Collections.emptyList();

        // 6. Daxil olan ödənişlərin dinamikası (son 30 təqvim günü, bu gün daxil)
        PaymentChartDto paymentChart = buildPaymentDynamicsChart();

        AdminDashboardDto dashboard = new AdminDashboardDto(totalOrders, pendingOrders, activeProducts, receivedPayments, recentOrders);
        dashboard.setPaymentChart(paymentChart);
        return dashboard;
    }

    private PaymentChartDto buildPaymentDynamicsChart() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = today.plusDays(1).atStartOfDay();

        List<Object[]> dailyResults = paymentRepository.sumDailyAmountByStatusAndDateRange(
                PaymentStatus.TAM_ÖDƏNİLMİŞ, startDateTime, endDateTime);

        Map<LocalDate, BigDecimal> dailyMap = new HashMap<>();
        if (dailyResults != null) {
            for (Object[] row : dailyResults) {
                if (row == null || row.length < 2 || row[0] == null) continue;
                LocalDate d = parseRowDate(row[0]);
                BigDecimal amt = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString());
                dailyMap.put(d, amt);
            }
        }

        DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("dd.MM");
        DateTimeFormatter fullFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        List<PaymentChartPointDto> points = new ArrayList<>(30);
        BigDecimal totalPeriodAmount = BigDecimal.ZERO;
        BigDecimal maxAmount = BigDecimal.ZERO;

        for (int i = 0; i < 30; i++) {
            LocalDate d = startDate.plusDays(i);
            BigDecimal amount = dailyMap.getOrDefault(d, BigDecimal.ZERO);
            totalPeriodAmount = totalPeriodAmount.add(amount);
            if (amount.compareTo(maxAmount) > 0) {
                maxAmount = amount;
            }
            points.add(new PaymentChartPointDto(
                    d,
                    d.format(displayFmt),
                    d.format(fullFmt),
                    amount,
                    String.format(Locale.US, "%.2f AZN", amount),
                    0.0,
                    0.0
            ));
        }

        boolean hasPayments = totalPeriodAmount.compareTo(BigDecimal.ZERO) > 0;

        // SVG Coordinate mapping (viewBox="0 0 800 240")
        final double xMin = 65.0;
        final double plotWidth = 710.0;
        final double yTop = 25.0;
        final double yBottom = 195.0;
        final double plotHeight = yBottom - yTop; // 170.0

        double yScaleMax = hasPayments ? (maxAmount.doubleValue() * 1.15) : 100.0;
        if (yScaleMax <= 0.0) {
            yScaleMax = 100.0;
        }

        StringBuilder linePath = new StringBuilder();
        StringBuilder areaPath = new StringBuilder();

        for (int i = 0; i < points.size(); i++) {
            PaymentChartPointDto pt = points.get(i);
            double x = xMin + (i * (plotWidth / 29.0));
            double y = yBottom;
            if (hasPayments) {
                double val = pt.getAmount().doubleValue();
                y = yBottom - ((val / yScaleMax) * plotHeight);
            }

            pt.setX(Math.round(x * 10.0) / 10.0);
            pt.setY(Math.round(y * 10.0) / 10.0);

            if (i == 0) {
                linePath.append(String.format(Locale.US, "M %.1f,%.1f", pt.getX(), pt.getY()));
                areaPath.append(String.format(Locale.US, "M %.1f,%.1f L %.1f,%.1f", pt.getX(), yBottom, pt.getX(), pt.getY()));
            } else {
                linePath.append(String.format(Locale.US, " L %.1f,%.1f", pt.getX(), pt.getY()));
                areaPath.append(String.format(Locale.US, " L %.1f,%.1f", pt.getX(), pt.getY()));
            }
        }
        areaPath.append(String.format(Locale.US, " L %.1f,%.1f Z", points.get(points.size() - 1).getX(), yBottom));

        // Y-axis gridlines & labels (4 levels: 0%, 33%, 66%, 100%)
        List<PaymentChartAxisLabelDto> yAxisLabels = new ArrayList<>();
        yAxisLabels.add(new PaymentChartAxisLabelDto(formatAxisAmount(yScaleMax), 60.0, yTop + 4.0));
        yAxisLabels.add(new PaymentChartAxisLabelDto(formatAxisAmount(yScaleMax * 2.0 / 3.0), 60.0, yTop + (plotHeight / 3.0) + 4.0));
        yAxisLabels.add(new PaymentChartAxisLabelDto(formatAxisAmount(yScaleMax / 3.0), 60.0, yTop + (plotHeight * 2.0 / 3.0) + 4.0));
        yAxisLabels.add(new PaymentChartAxisLabelDto("0 AZN", 60.0, yBottom + 4.0));

        // X-axis date labels (6 evenly spaced dates)
        List<PaymentChartAxisLabelDto> xAxisLabels = new ArrayList<>();
        int[] keyIndices = {0, 6, 12, 18, 24, 29};
        for (int idx : keyIndices) {
            PaymentChartPointDto pt = points.get(idx);
            xAxisLabels.add(new PaymentChartAxisLabelDto(pt.getFormattedDate(), pt.getX(), 218.0));
        }

        PaymentChartDto chartDto = new PaymentChartDto();
        chartDto.setHasPayments(hasPayments);
        chartDto.setTotalAmount(totalPeriodAmount);
        chartDto.setMaxAmount(maxAmount);
        chartDto.setPoints(points);
        chartDto.setYAxisLabels(yAxisLabels);
        chartDto.setXAxisLabels(xAxisLabels);
        chartDto.setSvgPathLine(linePath.toString());
        chartDto.setSvgPathArea(areaPath.toString());

        return chartDto;
    }

    private LocalDate parseRowDate(Object obj) {
        if (obj instanceof LocalDate ld) {
            return ld;
        }
        if (obj instanceof java.sql.Date sd) {
            return sd.toLocalDate();
        }
        if (obj instanceof java.util.Date ud) {
            return ud.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(obj.toString());
    }

    private String formatAxisAmount(double val) {
        if (val >= 1000.0) {
            return String.format(Locale.US, "%.1fk AZN", val / 1000.0);
        }
        return String.format(Locale.US, "%.0f AZN", val);
    }
}
