package com.library.pos.util.escpos;

import com.library.pos.util.escpos.ReceiptModels.Order;
import com.library.pos.util.escpos.ReceiptModels.OrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReceiptFormatter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final int width;
    private final String currency;
    private final NumberFormat moneyFormat;

    public ReceiptFormatter(ReceiptConfig config) {
        this.width = config.paperWidthChars();
        this.currency = config.currency();
        this.moneyFormat = NumberFormat.getNumberInstance(config.locale());
        this.moneyFormat.setMaximumFractionDigits(2);
        this.moneyFormat.setMinimumFractionDigits(2);
    }

    public List<String> format(Order order, boolean arabicPreferred) {
        List<String> out = new ArrayList<>();
        Labels labels = Labels.of(arabicPreferred);

        add(out, center(labels.storeName));
        add(out, center(labels.phone));
        add(out, hr());

        addIfPresent(out, center(pair(labels.invoiceNo, order.getInvoiceNo())));
        if (order.getDateTime() != null) {
            add(out, center(pair(labels.dateTime, DATE_TIME.format(order.getDateTime()))));
        }
        addIfPresent(out, center(pair(labels.cashier, order.getCashier())));
        addIfPresent(out, center(pair(labels.customerCode, order.getCustomerCode())));
        addIfPresent(out, center(pair("\u0627\u0633\u0645 \u0627\u0644\u0639\u0645\u064a\u0644:", order.getCustomerName())));
        addIfPresent(out, center(pair("\u0627\u0644\u0639\u0646\u0648\u0627\u0646:", order.getCustomerAddress())));
        addIfPresent(out, center(pair(labels.deliveredBy, order.getDeliveredBy())));
        addIfPresent(out, center(pair(labels.paymentMethod, order.getPaymentMethod())));

        add(out, hr());
        add(out, center(itemHeader(labels)));
        add(out, hr());

        for (OrderItem item : order.getItems()) {
            addAll(out, itemRows(item));
        }

        add(out, hr());
        add(out, center(rightPair(labels.subtotal, money(order.getSubtotal()))));

        if (isNonZero(order.getDiscount())) {
            add(out, center(rightPair(labels.discount, money(order.getDiscount()))));
        }
        if (isNonZero(order.getTax())) {
            add(out, center(rightPair(labels.tax, money(order.getTax()))));
        }

        add(out, center(rightPair(labels.total, money(order.getTotal()))));

        if (hasText(order.getPaymentMethod()) || order.getPaid() != null || order.getChange() != null) {
            add(out, hr());
            if (hasText(order.getPaymentMethod())) {
                add(out, center(pair(labels.paymentMethod, order.getPaymentMethod())));
            }
            if (order.getPaid() != null) {
                add(out, center(rightPair(labels.paid, money(order.getPaid()))));
            }
            if (order.getChange() != null) {
                add(out, center(rightPair(labels.change, money(order.getChange()))));
            }
        }

        add(out, hr());
        add(out, center(labels.thankYou));

        return trimTrailingEmpty(out);
    }

    public String center(String text) {
        String value = safe(text);
        if (value.length() >= width) {
            return cut(value, width);
        }
        int left = (width - value.length()) / 2;
        return rstrip(" ".repeat(Math.max(0, left)) + value);
    }

    public String hr() {
        return "-".repeat(Math.max(1, width));
    }

    public List<String> wrapName(String name, int nameWidth) {
        return wrapWords(safe(name), nameWidth);
    }

    private List<String> itemRows(OrderItem item) {
        int qtyW = width >= 48 ? 7 : 5;
        int unitW = width >= 48 ? 10 : 8;
        int totalW = width >= 48 ? 10 : 8;
        int nameW = width - (qtyW + unitW + totalW + 3);

        String qty = formatQty(item.getQty());
        String unit = moneyValue(item.getUnitPrice());
        String total = moneyValue(item.lineTotal());

        List<String> wrapped = wrapWords(safe(item.getName()), Math.max(8, nameW));
        List<String> rows = new ArrayList<>();

        for (int i = 0; i < wrapped.size(); i++) {
            String namePart = wrapped.get(i);
            String q = i == 0 ? qty : "";
            String u = i == 0 ? unit : "";
            String t = i == 0 ? total : "";

            String row = padRight(namePart, nameW)
                    + " " + padLeft(q, qtyW)
                    + " " + padLeft(u, unitW)
                    + " " + padLeft(t, totalW);
            rows.add(center(rstrip(cut(row, width))));
        }

        return rows;
    }

    private String itemHeader(Labels labels) {
        int qtyW = width >= 48 ? 7 : 5;
        int unitW = width >= 48 ? 10 : 8;
        int totalW = width >= 48 ? 10 : 8;
        int nameW = width - (qtyW + unitW + totalW + 3);
        String row = padRight(labels.item, nameW)
                + " " + padLeft(labels.qty, qtyW)
                + " " + padLeft(labels.unitPrice, unitW)
                + " " + padLeft(labels.lineTotal, totalW);
        return center(rstrip(cut(row, width)));
    }

    private String pair(String key, String value) {
        if (!hasText(value)) {
            return "";
        }
        return rstrip(cut(safe(key) + " " + safe(value), width));
    }

    private String rightPair(String key, String value) {
        String left = safe(key);
        String right = safe(value);
        int spaces = Math.max(1, width - left.length() - right.length());
        return rstrip(cut(left + " ".repeat(spaces) + right, width));
    }

    private String money(BigDecimal value) {
        return moneyValue(value) + (currency.isBlank() ? "" : " " + currency);
    }

    private String moneyValue(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return moneyFormat.format(v.setScale(2, RoundingMode.HALF_UP));
    }

    private String formatQty(BigDecimal qty) {
        BigDecimal v = qty == null ? BigDecimal.ZERO : qty;
        BigDecimal normalized = v.stripTrailingZeros();
        if (normalized.scale() <= 0) {
            return normalized.toPlainString();
        }
        int scale = Math.min(3, Math.max(0, normalized.scale()));
        return normalized.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static List<String> wrapWords(String value, int width) {
        List<String> parts = new ArrayList<>();
        if (!hasText(value)) {
            parts.add("");
            return parts;
        }

        String[] words = value.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.length() > width) {
                if (line.length() > 0) {
                    parts.add(line.toString());
                    line.setLength(0);
                }
                int start = 0;
                while (start < word.length()) {
                    int end = Math.min(start + width, word.length());
                    parts.add(word.substring(start, end));
                    start = end;
                }
                continue;
            }

            if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= width) {
                line.append(' ').append(word);
            } else {
                parts.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (line.length() > 0) {
            parts.add(line.toString());
        }
        return parts;
    }

    private static String cut(String text, int width) {
        if (text.length() <= width) {
            return text;
        }
        return text.substring(0, width);
    }

    private static String padLeft(String value, int width) {
        String t = safe(value);
        if (t.length() >= width) {
            return t.substring(0, width);
        }
        return " ".repeat(width - t.length()) + t;
    }

    private static String padRight(String value, int width) {
        String t = safe(value);
        if (t.length() >= width) {
            return t.substring(0, width);
        }
        return t + " ".repeat(width - t.length());
    }

    private static void add(List<String> out, String line) {
        if (line == null) {
            return;
        }
        out.add(rstrip(line));
    }

    private static void addIfPresent(List<String> out, String line) {
        if (hasText(line)) {
            add(out, line);
        }
    }

    private static void addAll(List<String> out, List<String> lines) {
        for (String line : lines) {
            add(out, line);
        }
    }

    private static List<String> trimTrailingEmpty(List<String> lines) {
        int end = lines.size();
        while (end > 0 && !hasText(lines.get(end - 1))) {
            end--;
        }
        return new ArrayList<>(lines.subList(0, end));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean isNonZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String rstrip(String value) {
        int i = value.length() - 1;
        while (i >= 0 && Character.isWhitespace(value.charAt(i))) {
            i--;
        }
        return value.substring(0, i + 1);
    }

    private static final class Labels {
        private final String storeName;
        private final String phone;
        private final String invoiceNo;
        private final String dateTime;
        private final String cashier;
        private final String customerCode;
        private final String deliveredBy;
        private final String paymentMethod;
        private final String item;
        private final String qty;
        private final String unitPrice;
        private final String lineTotal;
        private final String subtotal;
        private final String discount;
        private final String tax;
        private final String total;
        private final String paid;
        private final String change;
        private final String thankYou;

        private Labels(String storeName, String phone, String invoiceNo, String dateTime, String cashier,
                       String customerCode, String deliveredBy, String paymentMethod, String item,
                       String qty, String unitPrice, String lineTotal, String subtotal, String discount,
                       String tax, String total, String paid, String change, String thankYou) {
            this.storeName = storeName;
            this.phone = phone;
            this.invoiceNo = invoiceNo;
            this.dateTime = dateTime;
            this.cashier = cashier;
            this.customerCode = customerCode;
            this.deliveredBy = deliveredBy;
            this.paymentMethod = paymentMethod;
            this.item = item;
            this.qty = qty;
            this.unitPrice = unitPrice;
            this.lineTotal = lineTotal;
            this.subtotal = subtotal;
            this.discount = discount;
            this.tax = tax;
            this.total = total;
            this.paid = paid;
            this.change = change;
            this.thankYou = thankYou;
        }

        private static Labels of(boolean arabic) {
            if (arabic) {
                return new Labels(
                        "Ù…ÙƒØªØ¨Ù‡ Ø³Ù…Ø³Ù…",
                        "01064419197",
                        "Ø±Ù‚Ù… Ø§Ù„ÙØ§ØªÙˆØ±Ø©:",
                        "Ø§Ù„ØªØ§Ø±ÙŠØ®:",
                        "Ø§Ù„ÙƒØ§Ø´ÙŠØ±:",
                        "ÙƒÙˆØ¯ Ø§Ù„Ø¹Ù…ÙŠÙ„:",
                        "Ø¹Ø§Ù…Ù„ Ø§Ù„ØªÙˆØµÙŠÙ„:",
                        "Ø·Ø±ÙŠÙ‚Ø© Ø§Ù„Ø¯ÙØ¹:",
                        "Ø§Ù„ØµÙ†Ù",
                        "ÙƒÙ…",
                        "Ø§Ù„Ø³Ø¹Ø±",
                        "Ø§Ù„Ø¥Ø¬Ù…Ø§Ù„ÙŠ",
                        "Ø§Ù„Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„ÙØ±Ø¹ÙŠ",
                        "Ø§Ù„Ø®ØµÙ…",
                        "Ø§Ù„Ø¶Ø±ÙŠØ¨Ø©",
                        "Ø§Ù„Ø¥Ø¬Ù…Ø§Ù„ÙŠ Ø§Ù„Ù†Ù‡Ø§Ø¦ÙŠ",
                        "Ø§Ù„Ù…Ø¯ÙÙˆØ¹",
                        "Ø§Ù„Ø¨Ø§Ù‚ÙŠ",
                        "\u062A\u0634\u0631\u0641\u0646\u0627 \u0628\u0627\u0644\u062A\u0639\u0627\u0645\u0644 \u0645\u0639\u0643");
            }

            return new Labels(
                    "Sesame Library",
                    "01064419197",
                    "Invoice:",
                    "Date:",
                    "Cashier:",
                    "Customer Code:",
                    "Delivered By:",
                    "Payment:",
                    "Item",
                    "Qty",
                    "Unit",
                    "Total",
                    "Subtotal",
                    "Discount",
                    "Tax",
                    "Grand Total",
                    "Paid",
                    "Change",
                    "\u062A\u0634\u0631\u0641\u0646\u0627 \u0628\u0627\u0644\u062A\u0639\u0627\u0645\u0644 \u0645\u0639\u0643");
        }
    }
}
