package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.response.AdminDashboardDto;
import com.celiloglu.backend.dto.response.OrderResponseDto;
import com.celiloglu.backend.enums.OrderStatus;
import com.celiloglu.backend.enums.PaymentStatus;
import com.celiloglu.backend.repository.OrderRepository;
import com.celiloglu.backend.repository.PaymentRepository;
import com.celiloglu.backend.repository.ProductRepository;
import com.celiloglu.backend.service.impl.AdminDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderService orderService;

    private AdminDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new AdminDashboardServiceImpl(
                orderRepository,
                paymentRepository,
                productRepository,
                orderService
        );
    }

    @Test
    void getDashboardData_whenDataExists_shouldReturnPopulatedDto() {
        when(orderRepository.count()).thenReturn(42L);
        when(orderRepository.countByStatus(OrderStatus.GÖZLƏYİR)).thenReturn(7L);
        when(productRepository.countByIsActiveTrue()).thenReturn(150L);
        when(paymentRepository.sumAmountByStatus(PaymentStatus.TAM_ÖDƏNİLMİŞ))
                .thenReturn(new BigDecimal("12500.50"));

        LocalDate today = LocalDate.now();
        List<Object[]> dailyRows = List.of(
                new Object[]{today.minusDays(5), new BigDecimal("500.00")},
                new Object[]{today, new BigDecimal("250.00")}
        );
        when(paymentRepository.sumDailyAmountByStatusAndDateRange(eq(PaymentStatus.TAM_ÖDƏNİLMİŞ), any(), any()))
                .thenReturn(dailyRows);

        OrderResponseDto order1 = new OrderResponseDto();
        order1.setId(10L);
        order1.setOrderNumber("ORD-10");
        OrderResponseDto order2 = new OrderResponseDto();
        order2.setId(9L);
        order2.setOrderNumber("ORD-09");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(orderService.getAllOrdersForAdmin(pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(order1, order2)));

        AdminDashboardDto result = dashboardService.getDashboardData();

        assertNotNull(result);
        assertEquals(42L, result.getTotalOrders());
        assertEquals(7L, result.getPendingOrders());
        assertEquals(150L, result.getActiveProducts());
        assertEquals(new BigDecimal("12500.50"), result.getReceivedPayments());
        assertEquals(2, result.getRecentOrders().size());
        assertEquals("ORD-10", result.getRecentOrders().get(0).getOrderNumber());

        assertNotNull(result.getPaymentChart());
        assertEquals(30, result.getPaymentChart().getPoints().size());
        assertTrue(result.getPaymentChart().isHasPayments());
        assertEquals(new BigDecimal("750.00"), result.getPaymentChart().getTotalAmount());

        verify(orderRepository).countByStatus(OrderStatus.GÖZLƏYİR);

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(5, capturedPageable.getPageSize());
        Sort.Order orderDateSort = capturedPageable.getSort().getOrderFor("orderDate");
        Sort.Order idSort = capturedPageable.getSort().getOrderFor("id");
        assertNotNull(orderDateSort);
        assertEquals(Sort.Direction.DESC, orderDateSort.getDirection());
        assertNotNull(idSort);
        assertEquals(Sort.Direction.DESC, idSort.getDirection());
    }

    @Test
    void getDashboardData_whenEmptyDatabase_shouldReturnZeroesAndEmptyList() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.countByStatus(OrderStatus.GÖZLƏYİR)).thenReturn(0L);
        when(productRepository.countByIsActiveTrue()).thenReturn(0L);
        when(paymentRepository.sumAmountByStatus(PaymentStatus.TAM_ÖDƏNİLMİŞ)).thenReturn(null);
        when(paymentRepository.sumDailyAmountByStatusAndDateRange(eq(PaymentStatus.TAM_ÖDƏNİLMİŞ), any(), any()))
                .thenReturn(List.of());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(orderService.getAllOrdersForAdmin(pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of()));

        AdminDashboardDto result = dashboardService.getDashboardData();

        assertNotNull(result);
        assertEquals(0L, result.getTotalOrders());
        assertEquals(0L, result.getPendingOrders());
        assertEquals(0L, result.getActiveProducts());
        assertEquals(BigDecimal.ZERO, result.getReceivedPayments());
        assertNotNull(result.getRecentOrders());
        assertEquals(0, result.getRecentOrders().size());
        assertNotNull(result.getPaymentChart());
        assertEquals(30, result.getPaymentChart().getPoints().size());
        assertEquals(BigDecimal.ZERO, result.getPaymentChart().getTotalAmount());
    }
}
