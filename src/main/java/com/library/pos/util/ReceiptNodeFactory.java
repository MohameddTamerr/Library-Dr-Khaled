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
        VBox root = new VBox(5);
        root.getStyleClass().add("receipt-root");
        root.setPrefWidth(RECEIPT_WIDTH);
        root.setMaxWidth(RECEIPT_WIDTH);
        root.setAlignment(Pos.TOP_CENTER);

        if (arabic) {
            root.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        }

        // Ensure we load the stylesheet
        // root.getStylesheets().add(ReceiptNodeFactory.class.getResource("/css/receipt.css").toExternalForm());

        Labels labels = Labels.of(arabic);

        // 1. Header
        root.getChildren().addAll(
                styledLabel(labels.storeName, "shop-name"),
                styledLabel(labels.phone, "shop-phone"),
                new Separator());

        // 2. Meta Data (GridPane for alignment)
        GridPane metaGrid = new GridPane();
        metaGrid.setHgap(5);
        metaGrid.setVgap(2);
        metaGrid.setAlignment(Pos.CENTER);

        // In Arabic, we might want to align differently, but centered kv-pairs work
        // well
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

        // 3. Items Header
        GridPane itemHeader = createItemGrid(true, labels.item, labels.qty, labels.unitPrice, labels.lineTotal);
        itemHeader.getStyleClass().add("item-header");
        root.getChildren().add(itemHeader);

        root.getChildren().add(new Separator());

        // 4. Items List
        for (ReceiptModels.OrderItem item : order.getItems()) {
            GridPane itemRow = createItemGrid(false,
                    item.getName(),
                    formatQty(item.getQty()),
                    formatMoney(item.getUnitPrice()),
                    formatMoney(item.lineTotal()));
            itemRow.getStyleClass().add("item-row");
            root.getChildren().add(itemRow);
        }

        root.getChildren().add(new Separator());

        // 5. Totals
        VBox totalsBox = new VBox(2);
        totalsBox.getStyleClass().add("total-section");
        // Align totals based on orientation (Right for LTR, Left for RTL visually? No,
        // usually start aligned)
        totalsBox.setAlignment(Pos.CENTER_LEFT);

        totalsBox.getChildren().add(createTotalRow(labels.subtotal, formatMoney(order.getSubtotal())));

        if (isNonZero(order.getDiscount())) {
            totalsBox.getChildren().add(createTotalRow(labels.discount, formatMoney(order.getDiscount())));
        }
        if (isNonZero(order.getTax())) {
            totalsBox.getChildren().add(createTotalRow(labels.tax, formatMoney(order.getTax())));
        }

        // Grand Total (Bold)
        HBox totalRow = createTotalRow(labels.total, formatMoney(order.getTotal()));
        totalRow.getStyleClass().add("total-row");
        totalsBox.getChildren().add(totalRow);

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
        Label lbl = new Label(label);
        lbl.getStyleClass().add("header-label");
        Label val = new Label(value);
        row.getChildren().addAll(lbl, val);
        root.getChildren().add(row);
    }

    private static GridPane createItemGrid(boolean isHeader, String name, String qty, String price, String total) {
        GridPane grid = new GridPane();
        grid.setPrefWidth(RECEIPT_WIDTH);

        // Columns setup: Name (Flex), Qty (Fixed), Price (Fixed), Total (Fixed)
        ColumnConstraints colName = new ColumnConstraints();
        colName.setHgrow(Priority.ALWAYS);
        colName.setHalignment(javafx.geometry.HPos.LEFT); // Will correspond to RIGHT in RTL

        ColumnConstraints colQty = new ColumnConstraints();
        colQty.setPrefWidth(30);
        colQty.setHalignment(javafx.geometry.HPos.CENTER);

        ColumnConstraints colPrice = new ColumnConstraints();
        colPrice.setPrefWidth(50);
        colPrice.setHalignment(javafx.geometry.HPos.CENTER);

        ColumnConstraints colTotal = new ColumnConstraints();
        colTotal.setPrefWidth(50);
        colTotal.setHalignment(javafx.geometry.HPos.CENTER);

        grid.getColumnConstraints().addAll(colName, colQty, colPrice, colTotal);

        Label nameLbl = new Label(name);
        nameLbl.setWrapText(true);
        // Remove explicit text alignment to respect orientation
        // nameLbl.setTextAlignment(TextAlignment.LEFT);

        grid.add(nameLbl, 0, 0);
        grid.add(new Label(qty), 1, 0);
        grid.add(new Label(price), 2, 0);
        grid.add(new Label(total), 3, 0);

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
                        "مخبوزات المدينة المميزة والالبان", // From image "City Bakery & Dairy"? roughly
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
