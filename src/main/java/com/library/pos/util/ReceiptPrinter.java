package com.library.pos.util;

import com.library.pos.model.Customer;
import com.library.pos.model.Sale;
import com.library.pos.util.escpos.ReceiptModels;
import javafx.geometry.Insets;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReceiptPrinter {

    private static final String PRINTER_HINT = "Xprinter XP-370B";
    private static final DateTimeFormatter DEBUG_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReceiptPrinter() {
    }

    public static void previewSalesReceipt(Window owner, List<Sale> sales, String title) {
        if (sales == null || sales.isEmpty()) {
            return;
        }
        ReceiptModels.Order order = toOrder(sales);
        VBox receiptNode = ReceiptNodeFactory.createReceiptNode(order, true);

        // Apply CSS
        receiptNode.getStylesheets().add(ReceiptPrinter.class.getResource("/css/receipt.css").toExternalForm());

        ScrollPane scroll = new ScrollPane(receiptNode);
        scroll.setFitToWidth(false); // Allow actual width
        scroll.setPadding(new Insets(10));
        scroll.setStyle("-fx-background-color: transparent;");

        // Toolbar
        javafx.scene.layout.HBox toolbar = new javafx.scene.layout.HBox(10);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");

        javafx.scene.control.Button printBtn = new javafx.scene.control.Button("طباعة");
        printBtn.setStyle(
                "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("إغلاق");
        closeBtn.setStyle("-fx-cursor: hand;");

        toolbar.getChildren().addAll(printBtn, closeBtn);

        VBox root = new VBox(scroll, toolbar);
        javafx.scene.layout.VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        Scene scene = new Scene(root, 400, 700);
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("معاينة الفاتورة - " + title);
        stage.setScene(scene);

        // Actions
        printBtn.setOnAction(e -> {
            boolean success = printSalesReceipt(owner, sales, title);
            if (success) {
                stage.close();
            }
        });

        closeBtn.setOnAction(e -> stage.close());

        stage.show();
    }

    public static boolean printSalesReceipt(Window owner, List<Sale> sales, String title) {
        if (sales == null || sales.isEmpty()) {
            return false;
        }
        debug("printSalesReceipt start, sales=" + sales.size());

        ReceiptModels.Order order = toOrder(sales);
        VBox receiptNode = ReceiptNodeFactory.createReceiptNode(order, true);
        receiptNode.getStylesheets().add(ReceiptPrinter.class.getResource("/css/receipt.css").toExternalForm());

        // We need to layout the node to take a snapshot
        Scene dummy = new Scene(receiptNode);
        receiptNode.applyCss();
        receiptNode.layout();

        // 1. Try JavaFX print (Preferred)
        debug("Trying JavaFX print path.");
        if (printViaJavaFx(receiptNode)) {
            debug("JavaFX print success.");
            return true;
        }

        // 2. Fallback to AWT (Image-based)
        debug("JavaFX path failed. Trying AWT path.");
        WritableImage snapshot = receiptNode.snapshot(null, null);
        if (printViaAwt(snapshot)) {
            debug("AWT print success.");
            return true;
        }

        debug("AWT path failed.");
        return false;
    }

    private static boolean printViaJavaFx(VBox node) {
        Printer printer = resolvePreferredPrinter();
        if (printer == null) {
            return false;
        }

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) {
            return false;
        }

        // Setup page layout (80mm width usually)
        PageLayout pageLayout = printer.createPageLayout(
                job.getJobSettings().getPageLayout().getPaper(),
                PageOrientation.PORTRAIT,
                Printer.MarginType.HARDWARE_MINIMUM);
        job.getJobSettings().setPageLayout(pageLayout);

        double printableWidth = pageLayout.getPrintableWidth();
        double nodeWidth = node.getBoundsInParent().getWidth();
        double scaleFactor = 1.0;

        if (printableWidth > 0 && nodeWidth > 0) {
            // Scale content to fit printer width if needed, or scale up?
            // Usually receipts are small. If printableWidth is ~200px (58mm) vs ~280px
            // (80mm)
            scaleFactor = printableWidth / nodeWidth;
        }

        if (Math.abs(scaleFactor - 1.0) > 0.01) {
            node.getTransforms().add(new Scale(scaleFactor, scaleFactor));
        }

        boolean printed = job.printPage(pageLayout, node);
        if (printed) {
            job.endJob();
        }
        return printed;
    }

    private static boolean printViaAwt(WritableImage image) {
        // Convert WritableImage to BufferedImage if needed, or draw Image directly to
        // Graphics
        // For simplicity, we can reuse the Image approach but we need to convert JavaFX
        // Image to AWT
        // Or simpler: We just assume if JavaFX failed, AWT might work better with
        // simple strings?
        // No, we want to print the EXACT layout.
        // Converting JavaFX Image to BufferedImage requires SwingFXUtils which might be
        // in a separate module.
        // Let's try to just use the text-based AWT fallback? No, we want graphics.

        try {
            java.awt.image.BufferedImage bImage = javafx.embed.swing.SwingFXUtils.fromFXImage(image, null);

            PrintService service = resolveAwtPrinter();
            if (service == null)
                return false;

            java.awt.print.PrinterJob awtJob = java.awt.print.PrinterJob.getPrinterJob();
            awtJob.setPrintService(service);

            awtJob.setPrintable(new Printable() {
                @Override
                public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
                    if (pageIndex > 0)
                        return Printable.NO_SUCH_PAGE;

                    Graphics2D g2 = (Graphics2D) graphics;
                    g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                    double pWidth = pageFormat.getImageableWidth();
                    double iWidth = bImage.getWidth();
                    double scale = pWidth / iWidth;

                    // Maintain aspect ratio
                    int drawWidth = (int) (iWidth * scale);
                    int drawHeight = (int) (bImage.getHeight() * scale);

                    g2.drawImage(bImage, 0, 0, drawWidth, drawHeight, null);
                    return Printable.PAGE_EXISTS;
                }
            });

            awtJob.print();
            return true;
        } catch (Throwable ex) {
            debug("AWT print error: " + ex.getMessage());
            return false;
        }
    }

    private static PrintService resolveAwtPrinter() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services == null || services.length == 0) {
            return null;
        }
        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(PRINTER_HINT)) {
                return service;
            }
        }
        String hint = PRINTER_HINT.toLowerCase(Locale.ROOT);
        for (PrintService service : services) {
            String name = service.getName().toLowerCase(Locale.ROOT);
            if (name.contains(hint) || name.contains("xprinter") || name.contains("xp-370b")
                    || name.contains("xp370b")) {
                return service;
            }
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    private static Printer resolvePreferredPrinter() {
        for (Printer printer : Printer.getAllPrinters()) {
            if (printer == null || printer.getName() == null) {
                continue;
            }
            String name = printer.getName().toLowerCase(Locale.ROOT);
            if (name.contains("xprinter") || name.contains("xp-370b") || name.contains("xp370b")) {
                return printer;
            }
        }
        return Printer.getDefaultPrinter();
    }

    private static void debug(String message) {
        try {
            Path dir = Path.of("receipts");
            Files.createDirectories(dir);
            String line = "[" + LocalDateTime.now().format(DEBUG_TS) + "] " + message + System.lineSeparator();
            Files.writeString(
                    dir.resolve("print_debug.log"),
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private static ReceiptModels.Order toOrder(List<Sale> sales) {
        Sale first = sales.get(0);

        ReceiptModels.Order order = new ReceiptModels.Order();
        order.setInvoiceNo(first.getId() == null ? "-" : String.valueOf(first.getId()));
        order.setDateTime(first.getTimestamp());
        order.setCashier(first.getWorker() != null ? first.getWorker().getFullName() : "-");
        order.setCustomerCode(resolveCustomerCode(first.getCustomer()));
        order.setDeliveredBy(extractDeliveryWorker(first.getNotes()));
        order.setPaymentMethod(extractPaymentMethod(first.getNotes()));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Sale sale : sales) {
            if (sale == null) {
                continue;
            }

            BigDecimal qty = BigDecimal.valueOf(sale.getQuantity() == null ? 0 : sale.getQuantity());
            BigDecimal total = BigDecimal.valueOf(sale.getTotalAmount() == null ? 0.0 : sale.getTotalAmount());
            BigDecimal unit = qty.compareTo(BigDecimal.ZERO) == 0
                    ? total
                    : total.divide(qty, 4, RoundingMode.HALF_UP);

            ReceiptModels.OrderItem item = new ReceiptModels.OrderItem(
                    sale.getItemName() == null ? "" : sale.getItemName(),
                    qty,
                    unit);
            order.getItems().add(item);

            subtotal = subtotal.add(total);
        }

        String payment = order.getPaymentMethod() == null ? "" : order.getPaymentMethod().toLowerCase(Locale.ROOT);
        boolean deferred = payment.contains("deferred") || payment.contains("آجل");
        BigDecimal paid = deferred ? BigDecimal.ZERO : subtotal;
        BigDecimal change = BigDecimal.ZERO;

        order.setSubtotal(subtotal);
        order.setTotal(subtotal);
        order.setPaid(paid);
        order.setChange(change);

        return order;
    }

    private static String resolveCustomerCode(Customer customer) {
        if (customer == null) {
            return "عميل نقدي";
        }
        if (customer.getCustomerCode() != null && !customer.getCustomerCode().isBlank()) {
            return customer.getCustomerCode();
        }
        if (customer.getCustomerName() != null && !customer.getCustomerName().isBlank()) {
            return customer.getCustomerName();
        }
        return "عميل نقدي";
    }

    private static String extractPaymentMethod(String notes) {
        if (notes == null || notes.isBlank()) {
            return "CASH";
        }
        String base = notes.split("\\|", 2)[0].trim();
        String lower = base.toLowerCase(Locale.ROOT);
        if (lower.contains("payment")) {
            base = base.replace("Payment", "").trim();
        }
        if (lower.startsWith("deferred")) {
            return "DEFERRED";
        }
        return base.isBlank() ? "CASH" : base;
    }

    private static String extractDeliveryWorker(String notes) {
        if (notes == null || notes.isBlank()) {
            return "-";
        }
        String lower = notes.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("delivery:");
        if (idx == -1) {
            return "-";
        }
        String value = notes.substring(idx + "delivery:".length()).trim();
        int comma = value.indexOf(',');
        if (comma != -1) {
            value = value.substring(0, comma).trim();
        }
        return value.isBlank() ? "-" : value;
    }
}
