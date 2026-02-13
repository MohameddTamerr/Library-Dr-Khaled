package com.library.pos.util.escpos;

import com.library.pos.util.escpos.ReceiptModels.Order;
import com.library.pos.util.escpos.ReceiptModels.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptFormatterTest {

    @Test
    void linesStayWithinConfiguredWidthAndNoTrailingSpaces() {
        ReceiptConfig config = ReceiptConfig.builder()
                .paperWidthChars(32)
                .locale(Locale.forLanguageTag("ar-EG"))
                .build();

        Order order = new Order();
        order.setInvoiceNo("88");
        order.setDateTime(LocalDateTime.of(2026, 2, 13, 2, 4));
        order.setCashier("System Owner");
        order.setCustomerCode("4731");
        order.setPaymentMethod("Cash");
        order.getItems().add(new OrderItem("very very long product name to wrap safely", new BigDecimal("1"), new BigDecimal("25.00")));
        order.getItems().add(new OrderItem("asdf", new BigDecimal("2"), new BigDecimal("10.00")));
        order.setSubtotal(new BigDecimal("45.00"));
        order.setTotal(new BigDecimal("45.00"));

        ReceiptFormatter formatter = new ReceiptFormatter(config);
        List<String> lines = formatter.format(order, false);

        for (String line : lines) {
            assertTrue(line.length() <= 32, "Line exceeds width: [" + line + "]");
            assertFalse(line.endsWith(" "), "Line has trailing spaces: [" + line + "]");
        }
    }

    @Test
    void wrapsSingleLongWordWithHardSplit() {
        ReceiptConfig config = ReceiptConfig.builder().paperWidthChars(32).build();
        ReceiptFormatter formatter = new ReceiptFormatter(config);

        List<String> wrapped = formatter.wrapName("ABCDEFGHIJKL1234567890XYZ", 8);

        assertTrue(wrapped.size() >= 3);
        for (String part : wrapped) {
            assertTrue(part.length() <= 8);
        }
    }
}
