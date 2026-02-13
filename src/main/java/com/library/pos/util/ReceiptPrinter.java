package com.library.pos.util;

import com.library.pos.model.Customer;
import com.library.pos.model.Sale;
import com.library.pos.util.escpos.ReceiptConfig;
import com.library.pos.util.escpos.ReceiptFormatter;
import com.library.pos.util.escpos.ReceiptModels;
import javafx.geometry.VPos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public static boolean printSalesReceipt(Window owner, List<Sale> sales, String title) {
        if (sales == null || sales.isEmpty()) {
            return false;
        }
        debug("printSalesReceipt start, sales=" + sales.size());

        ReceiptModels.Order order = toOrder(sales);
        ReceiptConfig config = ReceiptConfig.builder()
                .paperWidthChars(32)
                .currency("ج.م")
                .locale(Locale.forLanguageTag("ar-EG"))
                .arabicPreferred(true)
                .enableCut(true)
                .enableDrawerKick(false)
                .build();

        try {
            List<String> lines = new ReceiptFormatter(config).format(order, config.arabicPreferred());
            debug("Trying JavaFX print path.");
            if (printViaJavaFx(lines)) {
                debug("JavaFX print success.");
                return true;
            }
            debug("JavaFX path failed. Trying AWT path.");
            if (printViaAwt(lines)) {
                debug("AWT print success.");
                return true;
            }
            debug("AWT path failed.");
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            debug("Print pipeline failed: " + ex.getMessage());
            return false;
        }
    }

    private static boolean printViaAwt(List<String> lines) {
        try {
            PrintService service = resolveAwtPrinter();
            if (service == null) {
                debug("AWT: no printer service found.");
                return false;
            }
            debug("AWT: selected printer = " + service.getName());

            java.awt.print.PrinterJob awtJob = java.awt.print.PrinterJob.getPrinterJob();
            awtJob.setPrintService(service);

            final List<String> safeLines = lines == null ? List.of("") : lines;
            awtJob.setPrintable(new Printable() {
                @Override
                public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
                    Graphics2D g2 = (Graphics2D) graphics;
                    g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                    g2.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 9));

                    int lineHeight = g2.getFontMetrics().getHeight();
                    int linesPerPage = Math.max(1, (int) (pageFormat.getImageableHeight() / lineHeight));
                    int start = pageIndex * linesPerPage;
                    if (start >= safeLines.size()) {
                        return Printable.NO_SUCH_PAGE;
                    }
                    int end = Math.min(safeLines.size(), start + linesPerPage);
                    int y = g2.getFontMetrics().getAscent();
                    for (int i = start; i < end; i++) {
                        String line = safeLines.get(i) == null ? "" : safeLines.get(i);
                        g2.drawString(line, 0, y);
                        y += lineHeight;
                    }
                    return Printable.PAGE_EXISTS;
                }
            });

            awtJob.print();
            return true;
        } catch (Exception ex) {
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
            if (name.contains(hint) || name.contains("xprinter") || name.contains("xp-370b") || name.contains("xp370b")) {
                return service;
            }
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    private static boolean printViaJavaFx(List<String> lines) {
        Printer printer = resolvePreferredPrinter();
        if (printer == null) {
            return false;
        }

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) {
            return false;
        }

        PageLayout selectedLayout = job.getJobSettings().getPageLayout();
        PageLayout pageLayout = printer.createPageLayout(
                selectedLayout.getPaper(),
                PageOrientation.PORTRAIT,
                Printer.MarginType.HARDWARE_MINIMUM);
        job.getJobSettings().setPageLayout(pageLayout);

        double printableWidth = pageLayout.getPrintableWidth();
        if (printableWidth <= 0) {
            printableWidth = 260.0;
        }
        double printableHeight = pageLayout.getPrintableHeight();
        if (printableHeight <= 0) {
            printableHeight = 420.0;
        }

        double lineHeight = 13.5;
        int linesPerPage = Math.max(1, (int) Math.floor((printableHeight - 2) / lineHeight));
        List<VBox> pages = new ArrayList<>();

        for (int start = 0; start < lines.size(); start += linesPerPage) {
            int end = Math.min(lines.size(), start + linesPerPage);
            int count = end - start;
            double canvasHeight = Math.max(1, count * lineHeight + 2);

            Canvas canvas = new Canvas(printableWidth, canvasHeight);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, printableWidth, canvasHeight);
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Cairo", 10));
            gc.setTextBaseline(VPos.TOP);

            double y = 1;
            for (int i = start; i < end; i++) {
                String line = lines.get(i) == null ? "" : lines.get(i);
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText(line, 1, y);
                y += lineHeight;
            }

            WritableImage snapshot = canvas.snapshot(new SnapshotParameters(), null);
            ImageView receiptImage = new ImageView(snapshot);
            receiptImage.setPreserveRatio(true);
            receiptImage.setFitWidth(printableWidth);

            VBox content = new VBox(receiptImage);
            new Scene(content);
            content.applyCss();
            content.layout();
            pages.add(content);
        }

        boolean printed = true;
        for (int i = pages.size() - 1; i >= 0; i--) {
            if (!job.printPage(pageLayout, pages.get(i))) {
                printed = false;
                break;
            }
        }
        if (printed) {
            job.endJob();
        }
        return printed;
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
