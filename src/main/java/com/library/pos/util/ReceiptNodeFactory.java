package com.library.pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

import com.library.pos.util.escpos.ReceiptModels;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class ReceiptNodeFactory {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final double RECEIPT_WIDTH = 290;
    private static final double MIN_RECEIPT_WIDTH = 220;

    public static VBox createReceiptNode(ReceiptModels.Order order, boolean arabic) {
        return createReceiptNode(order, arabic, RECEIPT_WIDTH);
    }

    public static VBox createReceiptNode(ReceiptModels.Order order, boolean arabic, double width) {
        double safeWidth = Math.max(MIN_RECEIPT_WIDTH, width);
        VBox root = new VBox(0); // Removing default spacing, controlling via padding/margins
        root.getStyleClass().add("receipt-root");
        root.setPrefWidth(safeWidth);
        root.setMaxWidth(safeWidth);
        root.setMinWidth(safeWidth);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new javafx.geometry.Insets(5, 2, 5, 2));

        if (arabic) {
            root.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        }

        Labels labels = Labels.of(arabic);

        // --- 1. Header Section ---
        VBox headerBox = new VBox(2);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.getChildren().add(styledLabel(labels.storeName, "shop-name"));
        if (hasText(labels.phone)) {
            headerBox.getChildren().add(styledLabel(labels.phone, "shop-phone"));
        }
        root.getChildren().add(headerBox);

        root.getChildren().add(new Separator());

        // --- 2. Meta Data Section ---
        VBox metaBox = new VBox(2);
        metaBox.getStyleClass().add("meta-section");
        metaBox.setFillWidth(true);
        metaBox.setMaxWidth(Double.MAX_VALUE);

        // Organize meta data in a clean vertical list or key-value pairs
        addMetaRow(metaBox, labels.invoiceNo, order.getInvoiceNo());
        if (order.getDateTime() != null) {
            addMetaRow(metaBox, labels.dateTime, DATE_TIME.format(order.getDateTime()));
        }
        addMetaRow(metaBox, labels.cashier, order.getCashier());

        // Optional fields
        if (hasText(order.getCustomerCode())) {
            addMetaRow(metaBox, labels.customerCode, order.getCustomerCode());
        }
        if (hasText(order.getCustomerName())) {
            addMetaRow(metaBox, "\u0627\u0633\u0645 \u0627\u0644\u0639\u0645\u064a\u0644:", order.getCustomerName());
        }
        if (hasText(order.getCustomerAddress())) {
            addMetaRow(metaBox, "\u0627\u0644\u0639\u0646\u0648\u0627\u0646:", order.getCustomerAddress());
        }
        if (hasText(order.getDeliveredBy())) {
            addMetaRow(metaBox, labels.deliveredBy, order.getDeliveredBy());
        }
        if (hasText(order.getPaymentMethod())) {
            addMetaRow(metaBox, labels.paymentMethod, order.getPaymentMethod());
        }
        root.getChildren().add(metaBox);

        root.getChildren().add(new Separator());

        // --- 3. Items Table ---
        VBox tableBox = new VBox();
        tableBox.getStyleClass().add("item-table");

        // Header
        GridPane itemHeader = createItemGrid(safeWidth, true, labels.item, labels.qty, labels.unitPrice, labels.lineTotal);
        itemHeader.getStyleClass().add("item-header-cell");
        tableBox.getChildren().add(itemHeader);

        // Items
        for (ReceiptModels.OrderItem item : order.getItems()) {
            GridPane itemRow = createItemGrid(safeWidth, false,
                    item.getName(),
                    formatQty(item.getQty()),
                    formatMoney(item.getUnitPrice()),
                    formatMoney(item.lineTotal()));
            tableBox.getChildren().add(itemRow);
        }
        root.getChildren().add(tableBox);

        root.getChildren().add(new Separator());

        // --- 4. Totals Section ---
        VBox totalsBox = new VBox(2);
        totalsBox.getStyleClass().add("total-section");
        totalsBox.setAlignment(Pos.CENTER);

        // Subtotal, Discount, Tax
        if (isNonZero(order.getSubtotal()) && order.getSubtotal().compareTo(order.getTotal()) != 0) {
            totalsBox.getChildren().add(createTotalRow(labels.subtotal, formatMoney(order.getSubtotal())));
        }
        if (isNonZero(order.getDiscount())) {
            totalsBox.getChildren().add(createTotalRow(labels.discount, formatMoney(order.getDiscount())));
        }
        if (isNonZero(order.getTax())) {
            totalsBox.getChildren().add(createTotalRow(labels.tax, formatMoney(order.getTax())));
        }

        // Boxed Total Due
        VBox totalDueBox = new VBox(5);
        totalDueBox.getStyleClass().add("total-due-box");
        totalDueBox.setAlignment(Pos.CENTER);
        totalDueBox.setPrefWidth(Math.max(150, safeWidth - 16));
        totalDueBox.setMaxWidth(Double.MAX_VALUE);
        totalDueBox.setMinWidth(Math.max(150, safeWidth - 16));
        VBox.setMargin(totalDueBox, new javafx.geometry.Insets(6, 4, 6, 4));

        Label requiredLabel = new Label("المطلوب");
        requiredLabel.getStyleClass().add("total-due-label");
        requiredLabel.setAlignment(Pos.CENTER);
        requiredLabel.setMaxWidth(Double.MAX_VALUE);
        requiredLabel.setTextAlignment(TextAlignment.CENTER);

        Label amountLabel = new Label(formatMoney(order.getTotal()));
        amountLabel.getStyleClass().add("total-due-amount");
        amountLabel.setAlignment(Pos.CENTER);
        amountLabel.setMaxWidth(Double.MAX_VALUE);
        amountLabel.setTextAlignment(TextAlignment.CENTER);

        totalDueBox.getChildren().addAll(requiredLabel, amountLabel);
        totalsBox.getChildren().add(totalDueBox);

        // Paid & Change
        if (order.getPaid() != null) {
            totalsBox.getChildren().add(createTotalRow(labels.paid, formatMoney(order.getPaid())));
        }
        if (order.getChange() != null) {
            totalsBox.getChildren().add(createTotalRow(labels.change, formatMoney(order.getChange())));
        }

        root.getChildren().add(totalsBox);

        // --- 5. Footer ---
        root.getChildren().add(new Separator());
        root.getChildren().add(styledLabel(labels.thankYou, "footer-label"));

        return root;
    }

    // --- Helpers ---

    private static void addMetaRow(VBox root, String label, String value) {
        if (!hasText(value))
            return;

        HBox row = new HBox(6);
        row.getStyleClass().add("meta-row");
        row.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setMaxWidth(Double.MAX_VALUE);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("meta-label");
        lbl.setMinWidth(Region.USE_PREF_SIZE);
        lbl.setAlignment(Pos.CENTER_RIGHT);
        lbl.setTextAlignment(TextAlignment.RIGHT);

        Label val = new Label(value);
        val.getStyleClass().add("meta-value");
        val.setWrapText(true);
        val.setAlignment(Pos.CENTER_RIGHT);
        val.setTextAlignment(TextAlignment.RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(spacer, val, lbl);
        root.getChildren().add(row);
    }

    private static GridPane createItemGrid(double width, boolean isHeader, String name, String qty, String price,
            String total) {
        double safeWidth = Math.max(MIN_RECEIPT_WIDTH, width);
        GridPane grid = new GridPane();
        grid.setPrefWidth(safeWidth);
        grid.setMaxWidth(safeWidth);
        grid.setMinWidth(safeWidth);
        // Force LTR on the grid itself — we manually order columns for RTL reading
        grid.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
        grid.setStyle("-fx-node-orientation: ltr;"); // Override CSS too

        // Visual order left-to-right: Total, Qty, Price, Name
        // Reading right-to-left: Name, Price, Qty, Total
        double totalW = Math.max(48, Math.round(safeWidth * 0.2d));
        double qtyW = Math.max(30, Math.round(safeWidth * 0.14d));
        double priceW = Math.max(48, Math.round(safeWidth * 0.2d));
        double nameW = Math.max(70, safeWidth - totalW - qtyW - priceW - 10);

        ColumnConstraints colTotal = new ColumnConstraints(totalW);
        colTotal.setHalignment(javafx.geometry.HPos.LEFT);

        ColumnConstraints colQty = new ColumnConstraints(qtyW);
        colQty.setHalignment(javafx.geometry.HPos.CENTER);

        ColumnConstraints colPrice = new ColumnConstraints(priceW);
        colPrice.setHalignment(javafx.geometry.HPos.CENTER);

        ColumnConstraints colName = new ColumnConstraints(nameW);
        colName.setHalignment(javafx.geometry.HPos.RIGHT);

        grid.getColumnConstraints().addAll(colTotal, colQty, colPrice, colName);

        // Styles
        String style = isHeader ? "header-label" : "cell-label";

        Label totalLbl = new Label(total);
        totalLbl.getStyleClass().add(style);
        totalLbl.setAlignment(Pos.CENTER_LEFT);
        totalLbl.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);

        Label qtyLbl = new Label(qty);
        qtyLbl.getStyleClass().add(style);
        qtyLbl.setAlignment(Pos.CENTER);
        qtyLbl.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);

        Label priceLbl = new Label(price);
        priceLbl.getStyleClass().add(style);
        priceLbl.setAlignment(Pos.CENTER);
        priceLbl.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);

        Label nameLbl = new Label(name);
        nameLbl.getStyleClass().add(style);
        nameLbl.setWrapText(false);
        nameLbl.setPrefWidth(nameW);
        nameLbl.setMinWidth(nameW);
        nameLbl.setMaxWidth(nameW);
        nameLbl.setAlignment(Pos.CENTER_RIGHT);
        nameLbl.setTextAlignment(TextAlignment.RIGHT);
        nameLbl.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);

        // Visual order left-to-right: Total(0), Qty(1), Price(2), Name(3)
        grid.add(totalLbl, 0, 0);
        grid.add(qtyLbl, 1, 0);
        grid.add(priceLbl, 2, 0);
        grid.add(nameLbl, 3, 0);

        return grid;
    }

    private static HBox createTotalRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("total-row-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label val = new Label(value);
        val.getStyleClass().add("total-row-value");

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
            this.setPrefHeight(5); // Reduced height as CSS handles detailed styling
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

    // --- Immutable Labels Class (Preserved) ---
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
                        "فرت-المدينه المنوره",
                        "فاتورة ضريبية مبسطة\nرقم تليفون: 01064419197 - 01062565115\nالعنوان: ابراج مدينه نصر عماره 3ب شارع المدارس",
                        "رقم الفاتورة:",
                        "التاريخ:",
                        "الكاشير:",
                        "العميل:",
                        "عامل التوصيل:",
                        "طريقة الدفع:",
                        "الصنف",
                        "كم",
                        "سعر",
                        "اجمالي",
                        "الاجمالي",
                        "الخصم",
                        "الضريبة",
                        "الاجمالي النهائي",
                        "المدفوع",
                        "المتبقي",
                        "\u062A\u0634\u0631\u0641\u0646\u0627 \u0628\u0627\u0644\u062A\u0639\u0627\u0645\u0644 \u0645\u0639\u0643");
            }
            return new Labels("Demo Shop", "123456", "Invoice:", "Date:", "Cashier:", "Customer:", "Driver:",
                    "Payment:",
                    "Item", "Qty", "Price", "Total", "Subtotal", "Discount", "Tax", "Total", "Paid", "Change",
                    "\u062A\u0634\u0631\u0641\u0646\u0627 \u0628\u0627\u0644\u062A\u0639\u0627\u0645\u0644 \u0645\u0639\u0643");
        }
    }
}
