package com.project.hsf.service;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderItem;
import com.project.hsf.entity.Payment;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.entity.User;
import com.project.hsf.enums.PaymentMethod;
import com.project.hsf.repository.CouponRepository;
import com.project.hsf.repository.OrderItemRepository;
import com.project.hsf.repository.OrderRepository;
import com.project.hsf.repository.OrderStatusHistoryRepository;
import com.project.hsf.repository.PaymentRepository;
import com.project.hsf.repository.SeafoodProductRepository;
import com.project.hsf.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServicePricingTest {

    @Mock
    private SeafoodProductRepository seafoodProductRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PayOS payOS;
    @Mock
    private CartService cartService;
    @Mock
    private PaymentService paymentService;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                seafoodProductRepository,
                couponRepository,
                orderRepository,
                orderItemRepository,
                orderStatusHistoryRepository,
                paymentRepository,
                payOS,
                cartService,
                paymentService);
    }

    @Test
    void placeOrderUsesDatabasePriceAndNameInsteadOfClientValues() {
        SeafoodProduct databaseProduct = SeafoodProduct.builder()
                .id(42L)
                .name("King Crab")
                .price(new BigDecimal("450000.00"))
                .stockQuantity(10)
                .active(true)
                .build();

        CartItemDTO tamperedCartItem = CartItemDTO.builder()
                .productId(42)
                .name("Cheap product")
                .unitPrice(1.0)
                .quantity(2)
                .build();

        when(seafoodProductRepository.deductStock(42L, 2)).thenReturn(1);
        when(seafoodProductRepository.findById(42L)).thenReturn(Optional.of(databaseProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.placeOrder(
                List.of(tamperedCartItem),
                null,
                "Ho Chi Minh City",
                PaymentMethod.COD.name(),
                null,
                "Nguyen Vinh Hung",
                "0842015248",
                new User());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        org.mockito.Mockito.verify(orderRepository).save(orderCaptor.capture());
        org.mockito.Mockito.verify(orderItemRepository).save(itemCaptor.capture());
        org.mockito.Mockito.verify(paymentRepository).save(paymentCaptor.capture());

        assertThat(orderCaptor.getValue().getTotalPrice()).isEqualByComparingTo("900000.00");
        assertThat(orderCaptor.getValue().getFinalPrice()).isEqualByComparingTo("900000.00");
        assertThat(itemCaptor.getValue().getProductName()).isEqualTo("King Crab");
        assertThat(itemCaptor.getValue().getUnitPrice()).isEqualByComparingTo("450000.00");
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("900000.00");
    }
}
