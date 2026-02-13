package com.library.pos.util;

import com.library.pos.util.escpos.ReceiptModels;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReceiptNodeFactory {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    // Approx 80mm printer width in pixels (assuming 203 DPI ~ 8 dots/mm) -> ~576px
    // But for JavaFX logical pixels (96 DPI), 80mm is approx 300px.
    // XPrinter XP-370B is often 76mm-80mm. Let's aim for ~280-300px width.
    private static final double RECEIPT_WIDTH = 290;

    public static VBox createReceiptNode(ReceiptModels.Order order, boolean arabic) {
        return createReceiptNode(order, arabic, RECEIPT_WIDTH);
    }

    public static VBox createReceiptNode(ReceiptModels.Order order, boolean arabic, double width) {
        VBox root = new VBox(5);
        root.getStyleClass().add("receipt-root");
        root.setPrefWidth(width);
        root.setMaxWidth(width);
        root.setAlignment(Pos.TOP_CENTER);

        if (arabic) {
            root.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        }

        Labels labels = Labels.of(arabic);

        // 1. Header
        root.getChildren().addAll(
                styledLabel(labels.storeName, "shop-name"),
                styledLabel(labels.phone, "shop-phone"),
                new Separator());

        // 2. Meta Data
        addMetaRow(root, labels.invoiceNo, order.getInvoiceNo());
        if (order.getDateTime() != null) {
            addMetaRow(root, labels.dateTime, DATE_TIME.format(order.getDateTime()));
        }
        addMetaRow(root, labels.cashier, order.getCashier());
        if (hasText(order.getCustomerCode())) {
            addMetaRow(root, labels.customerCode, order.getCustomerCode());
        }
        if (hasText(order.getDeliveredBy())) {
            addMetaRow(root, labels.deliveredBy, order.getDeliveredBy());
        }
        if (hasText(order.getPaymentMethod())) {
            addMetaRow(root, labels.paymentMethod, order.getPaymentMethod());
        }

        root.getChildren().add(new Separator());

        // 3. Items Table
        VBox tableBox = new VBox();
        tableBox.getStyleClass().add("item-table");

        // Header Row
        GridPane itemHeader = createItemGrid(width, true, labels.item, labels.qty, labels.unitPrice, labels.lineTotal);
        itemHeader.getStyleClass().add("item-header-cell");
        tableBox.getChildren().add(itemHeader);

        // Item Rows
        for (ReceiptModels.OrderItem item : order.getItems()) {
            GridPane itemRow = createItemGrid(width, false,
                    item.getName(),
                    formatQty(item.getQty()),
                    formatMoney(item.getUnitPrice()),
                    formatMoney(item.lineTotal()));
            tableBox.getChildren().add(itemRow);
        }
        root.getChildren().add(tableBox);

        // 4. Totals & Boxed "Required Amount"
        VBox totalsBox = new VBox(2);
        totalsBox.getStyleClass().add("total-section");
        totalsBox.setAlignment(Pos.CENTER);

        if (isNonZero(order.getSubtotal()) && order.getSubtotal().compareTo(order.getTotal()) != 0) {
            totalsBox.getChildren().add(createTotalRow(labels.subtotal, formatMoney(order.getSubtotal())));
        }

        if (isNonZero(order.getDiscount())) {
            totalsBox.getChildren().add(createTotalRow(labels.discount, formatMoney(order.getDiscount())));
        }
        if (isNonZero(order.getTax())) {
            totalsBox.getChildren().add(createTotalRow(labels.tax, formatMoney(order.getTax())));
        }

        // Boxed Total Due ("المطلوب")
        VBox totalDueBox = new VBox(5);
        totalDueBox.getStyleClass().add("total-due-box");
        Label requiredLabel = new Label("المطلوب");
        requiredLabel.getStyleClass().add("total-due-label");
        Label amountLabel = new Label(formatMoney(order.getTotal()));
        amountLabel.getStyleClass().add("total-due-amount");
        totalDueBox.getChildren().addAll(requiredLabel, amountLabel);
        totalsBox.getChildren().add(totalDueBox);

        if (order.getPaid() != null) {
            totalsBox.getChildren().add(createTotalRow(labels.paid, formatMoney(order.getPaid())));
        }
        if (order.getChange() != null) {
            totalsBox.getChildren().add(createTotalRow(labels.change, formatMoney(order.getChange())));
        }

        root.getChildren().add(totalsBox);

        // 6. Footer
        root.getChildren().add(new Separator());
        root.getChildren().add(styledLabel(labels.thankYou, "footer-label"));

        return root;
    }

    private static void addMetaRow(VBox root, String label, String value) {
        if (!hasText(value))
            return;
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(Double.MAX_VALUE);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("header-label");
        Label val = new Label(value);
        row.getChildren().addAll(lbl, val);
        root.getChildren().add(row);
    }

    private static GridPane createItemGrid(double width, boolean isHeader, String name, String qty, String price,
            String total) {
        GridPane grid = new GridPane();
        grid.setPrefWidth(width);
        grid.setMaxWidth(width);

        // Columns setup (RTL Perspective from right to left):
        // Col 0: Name (Flex)
        // Col 1: Price (Fixed)
        // Col 2: Qty (Fixed)
        // Col 3: Total (Fixed)

        ColumnConstraints colName = new ColumnConstraints();
        colName.setHgrow(Priority.ALWAYS);
        colName.setHalignment(javafx.geometry.HPos.RIGHT);

        ColumnConstraints colPrice = new ColumnConstraints();
        colPrice.setPrefWidth(55);
        colPrice.setHalignment(javafx.geometry.HPos.CENTER);

        ColumnConstraints colQty = new ColumnConstraints();
        colQty.setPrefWidth(35);
        colQty.setHalignment(javafx.geometry.HPos.CENTER);

        ColumnConstraints colTotal = new ColumnConstraints();
        colTotal.setPrefWidth(60);
        colTotal.setHalignment(javafx.geometry.HPos.LEFT);

        grid.getColumnConstraints().addAll(colName, colPrice, colQty, colTotal);

        Label nameLbl = new Label(name);
        nameLbl.setWrapText(true);
        nameLbl.setMaxWidth(Double.MAX_VALUE);
        nameLbl.setAlignment(Pos.CENTER_RIGHT);
        nameLbl.getStyleClass().add("cell-label");

        Label priceLbl = new Label(price);
        priceLbl.setMaxWidth(Double.MAX_VALUE);
        priceLbl.setAlignment(Pos.CENTER);
        priceLbl.getStyleClass().add("cell-label");

        Label qtyLbl = new Label(qty);
        qtyLbl.setMaxWidth(Double.MAX_VALUE);
        qtyLbl.setAlignment(Pos.CENTER);
        qtyLbl.getStyleClass().add("cell-label");

        Label totalLbl = new Label(total);
        totalLbl.setMaxWidth(Double.MAX_VALUE);
        totalLbl.setAlignment(Pos.CENTER_LEFT);
        totalLbl.getStyleClass().add("cell-label");

        grid.add(nameLbl, 0, 0);
        grid.add(priceLbl, 1, 0);
        grid.add(qtyLbl, 2, 0);
        grid.add(totalLbl, 3, 0);

        return grid;
    }

    private static HBox createTotalRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT); // Will be CENTER_RIGHT in RTL
        Label lbl = new Label(label);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label val = new Label(value);

        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }

    private static Label styledLabel(String text, String styleClass) {
        Label lbl = new Label(text);
        if (styleClass != null)
            lbl.getStyleClass().add(styleClass);
        lbl.setWrapText(true);
        lbl.setTextAlignment(TextAlignment.CENTER);
        lbl.setAlignment(Pos.CENTER);
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private static class Separator extends HBox {
        public Separator() {
            this.getStyleClass().add("dashed-line");
            this.setPrefHeight(10);
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean isNonZero(BigDecimal val) {
        return val != null && val.compareTo(BigDecimal.ZERO) != 0;
    }

    private static String formatMoney(BigDecimal val) {
        if (val == null)
            return "0.00";
        return val.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private static String formatQty(BigDecimal val) {
        if (val == null)
            return "0";
        if (val.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return String.valueOf(val.longValue());
        }
        return val.toString();
    }

    private static final class Labels {
        final String storeName;
        final String phone;
        final String invoiceNo;
        final String dateTime;
        final String cashier;
        final String customerCode;
        final String deliveredBy;
        final String paymentMethod;
        final String item;
        final String qty;
        final String unitPrice;
        final String lineTotal;
        final String subtotal;
        final String discount;
        final String tax;
        final String total;
        final String paid;
        final String change;
        final String thankYou;

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

        static Labels of(boolean arabic) {
            if (arabic) {
                return new Labels(
                        "مكتبة سمسم 2",
                        "فاتورة ضريبية مبسطة",
                        "رقم الفاتورة:",
                        "", // Date handled inside? Image shows just date string
                        "الكاشير:",
                        "العميل:",
                        "عامل التوصيل:",
                        "طريقة الدفع:",
                        "الصنف",
                        "كم",
                        "سعر",
                        "اجمالي",
                        "الاجمالي", // Subtotal
                        "الخصم",
                        "الضريبة",
                        "الاجمالي النهائي",
                        "المدفوع",
                        "المتبقي",
                        "");
            }
            return new Labels("Demo Shop", "123456", "Invoice:", "Date:", "Cashier:", "Customer:", "Driver:",
                    "Payment:",
                    "Item", "Qty", "Price", "Total", "Subtotal", "Discount", "Tax", "Total", "Paid", "Change",
                    "Thank You");
        }
    }
}
