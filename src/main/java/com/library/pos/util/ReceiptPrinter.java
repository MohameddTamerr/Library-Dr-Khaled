package com.library.pos.util;

import com.library.pos.model.Customer;
import com.library.pos.model.Sale;
import com.library.pos.util.escpos.EscPosReceiptPrinter;
import com.library.pos.util.escpos.ReceiptConfig;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.io.InputStream;

public final class ReceiptPrinter {

    private static final String PRINTER_HINT = "Xprinter XP-370B";
    private static final DateTimeFormatter DEBUG_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final PrintConfig PRINT_CONFIG = PrintConfig.load();
    private static final double DEFAULT_LAYOUT_WIDTH = 290.0d;
    private static final double MIN_LAYOUT_WIDTH = 220.0d;
    private static final double MAX_LAYOUT_WIDTH = 330.0d;
    private static final double POINTS_TO_FX = 96.0d / 72.0d;

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

        // Wrap receipt in a centered container
        javafx.scene.layout.StackPane centerWrapper = new javafx.scene.layout.StackPane(receiptNode);
        centerWrapper.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        centerWrapper.setPadding(new Insets(10));

        ScrollPane scroll = new ScrollPane(centerWrapper);
        scroll.setFitToWidth(true);
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

        // Resolve receipt node width in JavaFX pixels.
        double targetWidth = DEFAULT_LAYOUT_WIDTH;
        if (PRINT_CONFIG.receiptWidth != null && PRINT_CONFIG.receiptWidth > 0) {
            targetWidth = PRINT_CONFIG.receiptWidth;
            debug("Using manual receipt width override: " + targetWidth);
        } else {
            Printer printer = resolvePreferredPrinter();
            if (printer != null) {
                double detectedWidthPoints = printer.getDefaultPageLayout().getPrintableWidth();
                double detectedWidthFx = detectedWidthPoints * POINTS_TO_FX;
                if (detectedWidthFx >= MIN_LAYOUT_WIDTH) {
                    targetWidth = detectedWidthFx;
                    debug("Detected printer printable width: " + detectedWidthPoints + "pt (" + detectedWidthFx
                            + "px)");
                } else {
                    debug("Ignored too-small detected printer width: " + detectedWidthPoints + "pt ("
                            + detectedWidthFx + "px). Using default width.");
                }
            }
        }
        targetWidth = Math.max(MIN_LAYOUT_WIDTH, Math.min(MAX_LAYOUT_WIDTH, targetWidth));
        debug("Using receipt node width: " + targetWidth + "px");

        VBox receiptNode = ReceiptNodeFactory.createReceiptNode(order, true, targetWidth);
        receiptNode.getStylesheets().add(ReceiptPrinter.class.getResource("/css/receipt.css").toExternalForm());

        // We need to layout the node to take a snapshot
        Scene dummy = new Scene(receiptNode);
        receiptNode.applyCss();
        receiptNode.layout();

        PrintMode mode = PRINT_CONFIG.mode;
        debug("Print mode: " + mode);

        if (mode == PrintMode.ESC_POS) {
            debug("Trying ESC/POS raw path.");
            if (printViaEscPos(order)) {
                debug("ESC/POS print success.");
                return true;
            }
            debug("ESC/POS path failed.");
            return false;
        }

        // DEBUG: Save snapshot to checking rendering
        try {
            WritableImage debugSnap = receiptNode.snapshot(null, null);
            java.io.File debugFile = new java.io.File(
                    System.getProperty("user.home") + "/Desktop/debug_receipt_" + System.currentTimeMillis() + ".png");
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(debugSnap, null), "png", debugFile);
            debug("Saved debug receipt image to: " + debugFile.getAbsolutePath());
        } catch (Exception ex) {
            debug("Failed to save debug image: " + ex.getMessage());
        }

        if (mode == PrintMode.JAVAFX) {
            debug("Trying JavaFX print path.");
            if (printViaJavaFx(receiptNode)) {
                debug("JavaFX print success.");
                return true;
            }
            debug("JavaFX path failed. Falling back to AWT.");
            WritableImage snapshot = receiptNode.snapshot(null, null);
            if (printViaAwt(snapshot)) {
                debug("AWT print success.");
                return true;
            }
            debug("AWT path failed.");
            return false;
        }

        if (mode == PrintMode.AWT) {
            debug("Trying AWT print path.");
            // Explicitly set width again to be sure
            receiptNode.setPrefWidth(targetWidth);
            receiptNode.setMaxWidth(targetWidth);
            receiptNode.setMinWidth(targetWidth);
            receiptNode.layout();

            WritableImage snapshot = receiptNode.snapshot(null, null);
            if (printViaAwt(snapshot)) {
                debug("AWT print success.");
                return true;
            }
            debug("AWT path failed.");
            return false;
        }

        // AUTO
        debug("Trying JavaFX print path.");
        if (printViaJavaFx(receiptNode)) {
            debug("JavaFX print success.");
            return true;
        }

        debug("JavaFX path failed. Trying AWT path.");
        WritableImage snapshot = receiptNode.snapshot(null, null);
        if (printViaAwt(snapshot)) {
            debug("AWT print success.");
            return true;
        }

        debug("AWT path failed.");
        return false;
    }

    private static boolean printViaJavaFx(VBox nodeStub) {
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

        double printableWidthPoints = pageLayout.getPrintableWidth();
        double printableHeightPoints = pageLayout.getPrintableHeight();
        double printableWidthFx = printableWidthPoints * POINTS_TO_FX;
        double printableHeightFx = printableHeightPoints * POINTS_TO_FX;
        debug("JavaFX printable area: " + printableWidthPoints + "pt x " + printableHeightPoints
                + "pt (" + printableWidthFx + "px x " + printableHeightFx + "px)");

        if (printableWidthFx > 0) {
            double width = Math.max(MIN_LAYOUT_WIDTH, Math.min(MAX_LAYOUT_WIDTH, printableWidthFx));
            nodeStub.setPrefWidth(width);
            nodeStub.setMaxWidth(width);
            nodeStub.setMinWidth(width);
        }

        // Layout again
        if (nodeStub.getScene() == null) {
            new Scene(nodeStub);
        }
        nodeStub.applyCss();
        nodeStub.layout();
        double contentHeight = nodeStub.prefHeight(-1);
        if (printableHeightFx > 0 && contentHeight > printableHeightFx) {
            debug("JavaFX page height is too short for receipt content (" + contentHeight
                    + "px > " + printableHeightFx + "px).");
            return false;
        }

        try {
            boolean printed = job.printPage(pageLayout, nodeStub);
            if (printed) {
                job.endJob();
            }
            return printed;
        } catch (Exception ex) {
            debug("JavaFX print error: " + ex.getMessage());
            return false;
        }
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
            BufferedImage bImage = javafx.embed.swing.SwingFXUtils.fromFXImage(image, null);
            if (bImage == null) {
                return false;
            }

            // Flatten alpha to white to avoid printers rendering transparency as black
            // dots.
            BufferedImage printable = new BufferedImage(
                    bImage.getWidth(),
                    bImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D gFlatten = printable.createGraphics();
            gFlatten.setColor(Color.WHITE);
            gFlatten.fillRect(0, 0, printable.getWidth(), printable.getHeight());
            gFlatten.drawImage(bImage, 0, 0, null);
            gFlatten.dispose();

            PrintService service = resolveAwtPrinter();
            if (service == null)
                return false;

            java.awt.print.PrinterJob awtJob = java.awt.print.PrinterJob.getPrinterJob();
            awtJob.setPrintService(service);

            PageFormat pageFormat = awtJob.defaultPage();
            if (PRINT_CONFIG.awtPaperWidthMm != null && PRINT_CONFIG.awtPaperWidthMm > 0) {
                double widthPoints = mmToPoints(PRINT_CONFIG.awtPaperWidthMm);
                double scaleForHeight = widthPoints / printable.getWidth();
                double heightPoints = printable.getHeight() * scaleForHeight;
                if (PRINT_CONFIG.awtPaperHeightMm != null && PRINT_CONFIG.awtPaperHeightMm > 0) {
                    heightPoints = mmToPoints(PRINT_CONFIG.awtPaperHeightMm);
                } else {
                    heightPoints = Math.max(heightPoints + 10.0d, 200.0d);
                }

                double marginLeft = mmToPoints(PRINT_CONFIG.awtMarginLeftMm);
                double marginRight = mmToPoints(PRINT_CONFIG.awtMarginRightMm);
                double marginTop = mmToPoints(PRINT_CONFIG.awtMarginTopMm);
                double marginBottom = mmToPoints(PRINT_CONFIG.awtMarginBottomMm);
                double imageableWidth = Math.max(1.0d, widthPoints - marginLeft - marginRight);
                double imageableHeight = Math.max(1.0d, heightPoints - marginTop - marginBottom);

                java.awt.print.Paper paper = new java.awt.print.Paper();
                paper.setSize(widthPoints, heightPoints);
                paper.setImageableArea(marginLeft, marginTop, imageableWidth, imageableHeight);
                pageFormat.setPaper(paper);
            }

            Printable printableJob = new Printable() {
                @Override
                public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
                    if (pageIndex > 0)
                        return Printable.NO_SUCH_PAGE;

                    Graphics2D g2 = (Graphics2D) graphics;

                    double pWidth = pageFormat.getImageableWidth();
                    double iWidth = printable.getWidth();
                    double targetWidth = pWidth > 0 ? pWidth : iWidth;

                    double offsetX = mmToPoints(PRINT_CONFIG.awtOffsetXmm);
                    double offsetY = mmToPoints(PRINT_CONFIG.awtOffsetYmm);
                    g2.translate(pageFormat.getImageableX() + offsetX, pageFormat.getImageableY() + offsetY);

                    double scale = targetWidth / iWidth;
                    if (PRINT_CONFIG.awtScaleMultiplier != null && PRINT_CONFIG.awtScaleMultiplier > 0) {
                        scale *= PRINT_CONFIG.awtScaleMultiplier;
                    }
                    debug("AWT width: image=" + iWidth + "px, imageable=" + pWidth + "pt, scale=" + scale
                            + ", offsetX=" + offsetX + "pt, offsetY=" + offsetY + "pt");

                    // Maintain aspect ratio
                    int drawWidth = (int) (iWidth * scale);
                    int drawHeight = (int) (printable.getHeight() * scale);

                    g2.drawImage(printable, 0, 0, drawWidth, drawHeight, null);
                    return Printable.PAGE_EXISTS;
                }
            };

            // Ensure the custom PageFormat (paper size/margins) is used.
            awtJob.setPrintable(printableJob, pageFormat);

            awtJob.print();
            return true;
        } catch (Throwable ex) {
            debug("AWT print error: " + ex.getMessage());
            return false;
        }
    }

    private static boolean printViaEscPos(ReceiptModels.Order order) {
        try {
            ReceiptConfig.Builder builder = ReceiptConfig.builder();
            if (PRINT_CONFIG.paperWidthChars != null) {
                builder.paperWidthChars(PRINT_CONFIG.paperWidthChars);
            }
            if (PRINT_CONFIG.charset != null) {
                builder.charset(PRINT_CONFIG.charset);
            }
            if (PRINT_CONFIG.codeTable != null) {
                builder.codeTable(PRINT_CONFIG.codeTable);
            }
            if (PRINT_CONFIG.enableCut != null) {
                builder.enableCut(PRINT_CONFIG.enableCut);
            }
            if (PRINT_CONFIG.enableDrawerKick != null) {
                builder.enableDrawerKick(PRINT_CONFIG.enableDrawerKick);
            }
            if (PRINT_CONFIG.feedLinesBeforeCut != null) {
                builder.feedLinesBeforeCut(PRINT_CONFIG.feedLinesBeforeCut);
            }
            if (PRINT_CONFIG.arabicPreferred != null) {
                builder.arabicPreferred(PRINT_CONFIG.arabicPreferred);
            }

            ReceiptConfig config = builder.build();
            String printerName = printerHint();
            new EscPosReceiptPrinter().printOrderReceipt(order, config, printerName);
            return true;
        } catch (Throwable ex) {
            debug("ESC/POS print error: " + ex.getMessage());
            return false;
        }
    }

    private static PrintService resolveAwtPrinter() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services == null || services.length == 0) {
            return null;
        }
        String hint = printerHint();
        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(hint)) {
                return service;
            }
        }
        String hintLower = hint.toLowerCase(Locale.ROOT);
        for (PrintService service : services) {
            String name = service.getName().toLowerCase(Locale.ROOT);
            if (name.contains(hintLower) || name.contains("xprinter") || name.contains("xp-370b")
                    || name.contains("xp370b")) {
                return service;
            }
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    private static Printer resolvePreferredPrinter() {
        String hint = printerHint();
        for (Printer printer : Printer.getAllPrinters()) {
            if (printer == null || printer.getName() == null) {
                continue;
            }
            String name = printer.getName().toLowerCase(Locale.ROOT);
            if (name.contains(hint.toLowerCase(Locale.ROOT)) || name.contains("xprinter")
                    || name.contains("xp-370b") || name.contains("xp370b")) {
                return printer;
            }
        }
        return Printer.getDefaultPrinter();
    }

    private static String printerHint() {
        return PRINT_CONFIG.printerName == null || PRINT_CONFIG.printerName.isBlank()
                ? PRINTER_HINT
                : PRINT_CONFIG.printerName.trim();
    }

    private static double mmToPoints(double mm) {
        return (mm / 25.4d) * 72.0d;
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

    private enum PrintMode {
        AUTO,
        JAVAFX,
        AWT,
        ESC_POS
    }

    private static final class PrintConfig {
        final PrintMode mode;
        final String printerName;
        final Integer paperWidthChars;
        final Charset charset;
        final Integer codeTable;
        final Boolean enableCut;
        final Boolean enableDrawerKick;
        final Integer feedLinesBeforeCut;
        final Boolean arabicPreferred;
        final Integer awtPaperWidthMm;
        final Integer awtPaperHeightMm;
        final double awtMarginLeftMm;
        final double awtMarginRightMm;
        final double awtMarginTopMm;
        final double awtMarginBottomMm;
        final Double awtScaleMultiplier;
        final double awtOffsetXmm;
        final double awtOffsetYmm;
        final Double receiptWidth;

        private PrintConfig(PrintMode mode, String printerName, Integer paperWidthChars, Charset charset,
                Integer codeTable, Boolean enableCut, Boolean enableDrawerKick,
                Integer feedLinesBeforeCut, Boolean arabicPreferred, Integer awtPaperWidthMm,
                Integer awtPaperHeightMm, double awtMarginLeftMm, double awtMarginRightMm,
                double awtMarginTopMm, double awtMarginBottomMm, Double awtScaleMultiplier,
                double awtOffsetXmm, double awtOffsetYmm, Double receiptWidth) {
            this.mode = mode == null ? PrintMode.AUTO : mode;
            this.printerName = printerName;
            this.paperWidthChars = paperWidthChars;
            this.charset = charset;
            this.codeTable = codeTable;
            this.enableCut = enableCut;
            this.enableDrawerKick = enableDrawerKick;
            this.feedLinesBeforeCut = feedLinesBeforeCut;
            this.arabicPreferred = arabicPreferred;
            this.awtPaperWidthMm = awtPaperWidthMm;
            this.awtPaperHeightMm = awtPaperHeightMm;
            this.awtMarginLeftMm = awtMarginLeftMm;
            this.awtMarginRightMm = awtMarginRightMm;
            this.awtMarginTopMm = awtMarginTopMm;
            this.awtMarginBottomMm = awtMarginBottomMm;
            this.awtScaleMultiplier = awtScaleMultiplier;
            this.awtOffsetXmm = awtOffsetXmm;
            this.awtOffsetYmm = awtOffsetYmm;
            this.receiptWidth = receiptWidth;
        }

        static PrintConfig load() {
            Properties props = new Properties();
            try (InputStream in = ReceiptPrinter.class.getResourceAsStream("/application.properties")) {
                if (in != null) {
                    props.load(in);
                }
            } catch (Exception ignored) {
            }

            PrintMode mode = parseMode(props.getProperty("receipt.print.mode"));
            String printerName = trimToNull(props.getProperty("receipt.printer.name"));
            Integer paperWidthChars = parseInt(props.getProperty("receipt.escpos.paper_width_chars"));
            Charset charset = parseCharset(props.getProperty("receipt.escpos.charset"));
            Integer codeTable = parseInt(props.getProperty("receipt.escpos.code_table"));
            Boolean enableCut = parseBoolean(props.getProperty("receipt.escpos.enable_cut"));
            Boolean enableDrawerKick = parseBoolean(props.getProperty("receipt.escpos.enable_drawer_kick"));
            Integer feedLinesBeforeCut = parseInt(props.getProperty("receipt.escpos.feed_lines_before_cut"));
            Boolean arabicPreferred = parseBoolean(props.getProperty("receipt.escpos.arabic_preferred"));
            Integer awtPaperWidthMm = parseInt(props.getProperty("receipt.awt.paper_width_mm"));
            Integer awtPaperHeightMm = parseInt(props.getProperty("receipt.awt.paper_height_mm"));
            double marginLeft = parseDouble(props.getProperty("receipt.awt.margin_left_mm"), 1.0d);
            double marginRight = parseDouble(props.getProperty("receipt.awt.margin_right_mm"), 1.0d);
            double marginTop = parseDouble(props.getProperty("receipt.awt.margin_top_mm"), 1.0d);
            double marginBottom = parseDouble(props.getProperty("receipt.awt.margin_bottom_mm"), 1.0d);
            Double scaleMultiplier = parseOptionalDouble(props.getProperty("receipt.awt.scale_multiplier"));
            double offsetX = parseDouble(props.getProperty("receipt.awt.offset_x_mm"), 0.0d);
            double offsetY = parseDouble(props.getProperty("receipt.awt.offset_y_mm"), 0.0d);
            Double receiptWidth = parseOptionalDouble(props.getProperty("receipt.width"));

            return new PrintConfig(mode, printerName, paperWidthChars, charset, codeTable,
                    enableCut, enableDrawerKick, feedLinesBeforeCut, arabicPreferred, awtPaperWidthMm,
                    awtPaperHeightMm, marginLeft, marginRight, marginTop, marginBottom, scaleMultiplier,
                    offsetX, offsetY, receiptWidth);
        }

        private static PrintMode parseMode(String value) {
            if (value == null || value.isBlank()) {
                return PrintMode.AUTO;
            }
            String v = value.trim().toUpperCase(Locale.ROOT);
            return switch (v) {
                case "JAVAFX" -> PrintMode.JAVAFX;
                case "AWT" -> PrintMode.AWT;
                case "ESC_POS", "ESCPOS", "ESC-POS" -> PrintMode.ESC_POS;
                default -> PrintMode.AUTO;
            };
        }

        private static Integer parseInt(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (Exception ignored) {
                return null;
            }
        }

        private static Boolean parseBoolean(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String v = value.trim().toLowerCase(Locale.ROOT);
            if (v.equals("true") || v.equals("1") || v.equals("yes")) {
                return true;
            }
            if (v.equals("false") || v.equals("0") || v.equals("no")) {
                return false;
            }
            return null;
        }

        private static Charset parseCharset(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return Charset.forName(value.trim());
            } catch (Exception ignored) {
                return null;
            }
        }

        private static double parseDouble(String value, double fallback) {
            if (value == null || value.isBlank()) {
                return fallback;
            }
            try {
                return Double.parseDouble(value.trim());
            } catch (Exception ignored) {
                return fallback;
            }
        }

        private static Double parseOptionalDouble(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return Double.parseDouble(value.trim());
            } catch (Exception ignored) {
                return null;
            }
        }

        private static String trimToNull(String value) {
            if (value == null) {
                return null;
            }
            String v = value.trim();
            return v.isBlank() ? null : v;
        }
    }
}
