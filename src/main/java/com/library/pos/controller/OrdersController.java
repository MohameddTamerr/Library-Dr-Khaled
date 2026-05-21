package com.library.pos.controller;

import com.library.pos.model.Sale;
import com.library.pos.model.SaleStatus;
import com.library.pos.service.SaleService;
import com.library.pos.util.AutoRefreshUtil;
import com.library.pos.util.ReceiptPrinter;
import javafx.stage.FileChooser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

@Component
public class OrdersController {

    @FXML
    private TreeTableView<OrderViewModel> ordersTable;
    @FXML
    private TreeTableColumn<OrderViewModel, String> idCol;
    @FXML
    private TreeTableColumn<OrderViewModel, Void> printCol;
    @FXML
    private TreeTableColumn<OrderViewModel, String> timeCol;
    @FXML
    private TreeTableColumn<OrderViewModel, String> itemCol;
    @FXML
    private TreeTableColumn<OrderViewModel, Integer> quantityCol;
    @FXML
    private TreeTableColumn<OrderViewModel, String> notesCol;
    @FXML
    private TreeTableColumn<OrderViewModel, String> paymentCol;
    @FXML
    private TreeTableColumn<OrderViewModel, String> deliveryManCol;

    @FXML
    private TreeTableColumn<OrderViewModel, String> dateCol;
    @FXML
    private TreeTableColumn<OrderViewModel, String> customerCol;
    @FXML
    private TreeTableColumn<OrderViewModel, String> workerCol;
    @FXML
    private TreeTableColumn<OrderViewModel, Double> amountCol;
    @FXML
    private TreeTableColumn<OrderViewModel, String> statusCol;

    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private javafx.scene.control.TextField filterProductField;
    @FXML
    private javafx.scene.control.ComboBox<com.library.pos.model.User> filterWorkerCombo;
    @FXML
    private javafx.scene.control.ComboBox<String> filterOrderTypeCombo;

    // Payment Method Summary Labels
    @FXML
    private javafx.scene.control.Label cashTotalLabel;
    @FXML
    private javafx.scene.control.Label instapayTotalLabel;
    @FXML
    private javafx.scene.control.Label visaTotalLabel;
    @FXML
    private javafx.scene.control.Label vodafoneTotalLabel;
    @FXML
    private javafx.scene.control.Label deliveryCountLabel;
    @FXML
    private javafx.scene.control.Label shopCountLabel;
    @FXML
    private javafx.scene.control.Label totalOrdersLabel;

    private final SaleService saleService;
    private final com.library.pos.service.UserService userService;
    private Timeline autoRefreshTimeline;
    private static final int REFRESH_SECONDS_VISIBLE = 3;
    private static final int REFRESH_SECONDS_HIDDEN = 6;
    private LocalDateTime lastSaleTimestamp;
    private long lastSaleCount = -1;
    // Formatters can be static or instance, but now used locally in OrderViewModel
    // kept here if needed for other things, but OrderViewModel now has its own.

    public OrdersController(SaleService saleService, com.library.pos.service.UserService userService) {
        this.saleService = saleService;
        this.userService = userService;
    }

    @FXML
    public void initialize() {
        // ID Column
        idCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getId()));
        if (printCol != null) {
            printCol.setCellValueFactory(param -> new SimpleObjectProperty<>(null));
            printCol.setCellFactory(col -> new TreeTableCell<>() {
                private final Button printBtn = new Button("طباعة");

                {
                    printBtn.getStyleClass().add("button-primary");
                    printBtn.getStyleClass().add("button-print");
                    printBtn.setMinWidth(70);
                    printBtn.setPrefWidth(70);
                    printBtn.setMaxWidth(Double.MAX_VALUE);
                    printBtn.setOnAction(event -> {
                        TreeItem<OrderViewModel> rowItem = getTreeTableRow() != null ? getTreeTableRow().getTreeItem()
                                : null;
                        if (rowItem == null || rowItem.getValue() == null || !rowItem.getValue().isOrderHeader()) {
                            return;
                        }
                        List<Sale> orderSales = rowItem.getValue().getOrderSales();
                        if (orderSales == null || orderSales.isEmpty()) {
                            return;
                        }
                        ReceiptPrinter.previewSalesReceipt(
                                ordersTable != null && ordersTable.getScene() != null
                                        ? ordersTable.getScene().getWindow()
                                        : null,
                                orderSales,
                                "فاتورة طلب سابق");
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    TreeItem<OrderViewModel> rowItem = getTreeTableRow() != null ? getTreeTableRow().getTreeItem()
                            : null;
                    boolean show = !empty
                            && rowItem != null
                            && rowItem.getValue() != null
                            && rowItem.getValue().isOrderHeader();
                    setGraphic(show ? printBtn : null);
                    setText(null);
                }
            });
        }
        dateCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getDate()));
        timeCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getTime()));
        itemCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getItem()));
        quantityCol.setCellValueFactory(
                param -> new SimpleIntegerProperty(param.getValue().getValue().getQuantity()).asObject());
        customerCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getCustomer()));
        workerCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getWorker()));
        amountCol.setCellValueFactory(
                param -> new SimpleDoubleProperty(param.getValue().getValue().getAmount()).asObject());
        statusCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getStatus()));
        paymentCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getPayment()));
        deliveryManCol
                .setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getDeliveryMan()));
        notesCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getNotes()));

        // Context Menu for Preview
        ordersTable.setRowFactory(tv -> {
            javafx.scene.control.TreeTableRow<OrderViewModel> row = new javafx.scene.control.TreeTableRow<>();
            javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
            javafx.scene.control.MenuItem previewItem = new javafx.scene.control.MenuItem("معاينة الفاتورة");
            previewItem.setOnAction(event -> {
                OrderViewModel item = row.getItem();
                if (item != null && item.isOrderHeader() && item.getOrderSales() != null
                        && !item.getOrderSales().isEmpty()) {
                    ReceiptPrinter.previewSalesReceipt(
                            ordersTable.getScene().getWindow(),
                            item.getOrderSales(),
                            "فاتورة " + item.getId());
                }
            });
            menu.getItems().add(previewItem);

            // Only show for non-empty rows that are Order Headers
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(() -> {
                if (row.isEmpty() || row.getItem() == null || !row.getItem().isOrderHeader()) {
                    return null;
                }
                return menu;
            }, row.itemProperty(), row.emptyProperty()));
            return row;
        });

        // Setup filter combo
        filterWorkerCombo.setConverter(new javafx.util.StringConverter<com.library.pos.model.User>() {
            @Override
            public String toString(com.library.pos.model.User user) {
                return user == null ? "الكل" : user.getFullName();
            }

            @Override
            public com.library.pos.model.User fromString(String string) {
                return null; // Not needed
            }
        });
        filterWorkerCombo.setItems(FXCollections.observableArrayList());
        filterWorkerCombo.getItems().add(null); // Option for "All"
        filterWorkerCombo.getItems().addAll(userService.getAllWorkers());
        filterWorkerCombo.getSelectionModel().selectFirst();

        // Order type filter
        if (filterOrderTypeCombo != null) {
            filterOrderTypeCombo.setItems(FXCollections.observableArrayList(
                    "الكل",
                    "توصيل",
                    "داخل المحل"));
            filterOrderTypeCombo.getSelectionModel().selectFirst();
        }

        // Default: Today
        fromDatePicker.setValue(LocalDate.now());
        toDatePicker.setValue(LocalDate.now());

        // Initial Load
        handleFilter();
        updateDataSignature();
        setupAutoRefresh();
    }

    private void setupAutoRefresh() {
        if (ordersTable == null) {
            return;
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(REFRESH_SECONDS_VISIBLE), e -> {
            if (shouldRefresh()) {
                handleFilter();
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
        AutoRefreshUtil.bind(autoRefreshTimeline, ordersTable,
                (double) REFRESH_SECONDS_VISIBLE / (double) REFRESH_SECONDS_HIDDEN);
    }

    private boolean shouldRefresh() {
        if (isUserEditing()) {
            return false;
        }
        return hasDataChanged();
    }

    private boolean isUserEditing() {
        return (filterProductField != null && filterProductField.isFocused())
                || (fromDatePicker != null && fromDatePicker.isFocused())
                || (toDatePicker != null && toDatePicker.isFocused())
                || (filterWorkerCombo != null && filterWorkerCombo.isFocused())
                || (filterOrderTypeCombo != null && filterOrderTypeCombo.isFocused());
    }

    private boolean hasDataChanged() {
        LocalDateTime latest = saleService.getLatestSaleTimestamp();
        long count = saleService.getTotalCount();
        boolean changed = !Objects.equals(latest, lastSaleTimestamp) || count != lastSaleCount;
        if (changed) {
            lastSaleTimestamp = latest;
            lastSaleCount = count;
        }
        return changed;
    }

    private void updateDataSignature() {
        lastSaleTimestamp = saleService.getLatestSaleTimestamp();
        lastSaleCount = saleService.getTotalCount();
    }

    @FXML
    public void handleFilter() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        // Get filters
        String productName = filterProductField != null ? filterProductField.getText() : null;
        if (productName != null && productName.trim().isEmpty())
            productName = null;

        com.library.pos.model.User worker = filterWorkerCombo.getValue();
        Long workerId = worker != null ? worker.getId() : null;

        List<Sale> sales = new ArrayList<>();

        if (from != null && to != null) {
            LocalDateTime start = from.atStartOfDay();
            LocalDateTime end = to.atTime(LocalTime.MAX);
            sales = saleService.search(start, end, workerId, productName);
        }

        sales = applyOrderTypeFilter(sales);
        updatePaymentMethodTotals(sales);
        updateOrderCounts(sales);

        // Populate Tree
        populateTree(sales);
        updateDataSignature();
    }

    private void populateTree(List<Sale> sales) {
        TreeItem<OrderViewModel> root = new TreeItem<>(new OrderViewModel((Sale) null));
        root.setExpanded(true);

        // Group by Timestamp + Worker to simulate "Orders"
        // Key concept: Sales with exact same timestamp and worker are one order
        Map<String, List<Sale>> groupedSales = sales.stream()
                .collect(Collectors
                        .groupingBy(s -> (s.getTimestamp() != null ? s.getTimestamp().toString() : "NULL") + "_" +
                                (s.getWorker() != null ? s.getWorker().getId() : "0") + "_" +
                                // Also group by customer if present to prevent mixing
                                (s.getCustomer() != null ? s.getCustomer().getId() : "CASH")));

        // Sort groups by date descending
        groupedSales.entrySet().stream()
                .sorted((e1, e2) -> {
                    // Peek first element to compare timestamps
                    Sale s1 = e1.getValue().get(0);
                    Sale s2 = e2.getValue().get(0);
                    if (s1.getTimestamp() == null)
                        return 1;
                    if (s2.getTimestamp() == null)
                        return -1;
                    return s2.getTimestamp().compareTo(s1.getTimestamp());
                })
                .forEach(entry -> {
                    List<Sale> orderItems = entry.getValue();
                    Sale first = orderItems.get(0);

                    // Create Order Header
                    OrderViewModel header = new OrderViewModel(orderItems);
                    TreeItem<OrderViewModel> orderNode = new TreeItem<>(header);

                    // Create Item Nodes
                    for (Sale item : orderItems) {
                        TreeItem<OrderViewModel> itemNode = new TreeItem<>(new OrderViewModel(item));
                        orderNode.getChildren().add(itemNode);
                    }

                    root.getChildren().add(orderNode);
                });

        ordersTable.setRoot(root);
        ordersTable.setShowRoot(false);
    }

    private void updatePaymentMethodTotals(List<Sale> sales) {
        double cashTotal = 0, instapayTotal = 0, visaTotal = 0, vodafoneTotal = 0;

        for (Sale sale : sales) {
            String notes = sale.getNotes();
            if (notes == null) {
                notes = "";
            }
            String notesLower = notes.toLowerCase(Locale.ROOT);
            double amount = sale.getTotalAmount() != null ? sale.getTotalAmount() : 0;

            if (notesLower.contains("instapay")) {
                instapayTotal += amount;
            } else if (notesLower.contains("visa")) {
                visaTotal += amount;
            } else if (notesLower.contains("vodafone")) {
                vodafoneTotal += amount;
            } else if (notesLower.contains("cash") || notes.contains("نقدي")) {
                cashTotal += amount;
            } else {
                // Default to cash if no payment method specified
                cashTotal += amount;
            }
        }

        // Update labels
        if (cashTotalLabel != null)
            cashTotalLabel.setText(String.format("%.2f ج.م", cashTotal));
        if (instapayTotalLabel != null)
            instapayTotalLabel.setText(String.format("%.2f ج.م", instapayTotal));
        if (visaTotalLabel != null)
            visaTotalLabel.setText(String.format("%.2f ج.م", visaTotal));
        if (vodafoneTotalLabel != null)
            vodafoneTotalLabel.setText(String.format("%.2f ج.م", vodafoneTotal));
    }

    private void updateOrderCounts(List<Sale> sales) {
        Map<String, List<Sale>> groupedSales = sales.stream()
                .collect(Collectors.groupingBy(s -> (s.getTimestamp() != null ? s.getTimestamp().toString() : "NULL")
                        + "_" + (s.getWorker() != null ? s.getWorker().getId() : "0")
                        + "_" + (s.getCustomer() != null ? s.getCustomer().getId() : "CASH")));

        long deliveryCount = groupedSales.values().stream()
                .filter(this::isDeliveryOrder)
                .count();
        long totalCount = groupedSales.size();
        long shopCount = Math.max(0, totalCount - deliveryCount);

        if (deliveryCountLabel != null)
            deliveryCountLabel.setText(String.valueOf(deliveryCount));
        if (shopCountLabel != null)
            shopCountLabel.setText(String.valueOf(shopCount));
        if (totalOrdersLabel != null)
            totalOrdersLabel.setText(String.valueOf(totalCount));
    }

    private List<Sale> applyOrderTypeFilter(List<Sale> sales) {
        if (filterOrderTypeCombo == null || filterOrderTypeCombo.getValue() == null) {
            return sales;
        }
        String type = filterOrderTypeCombo.getValue();
        if ("توصيل".equals(type)) {
            return sales.stream().filter(this::isDelivery).collect(Collectors.toList());
        }
        if ("داخل المحل".equals(type)) {
            return sales.stream().filter(s -> !isDelivery(s)).collect(Collectors.toList());
        }
        return sales;
    }

    private boolean isDelivery(Sale sale) {
        if (sale == null) {
            return false;
        }
        if (sale.getStatus() == com.library.pos.model.SaleStatus.DELIVERY) {
            return true;
        }
        // Check notes for "Delivery:" if status is DEFERRED or SOLD
        String notes = sale.getNotes();
        return notes != null && notes.toLowerCase(Locale.ROOT).contains("delivery:");
    }

    private boolean isDeliveryOrder(List<Sale> sales) {
        return sales != null && sales.stream().anyMatch(this::isDelivery);
    }

    @FXML
    public void loadAll() {
        List<Sale> sales = saleService.getAll();
        updatePaymentMethodTotals(sales);
        updateOrderCounts(sales);
        populateTree(sales);
    }

    @FXML
    private void handleExportSoldProductsPdf() {
        LocalDate from = fromDatePicker != null ? fromDatePicker.getValue() : null;
        LocalDate to = toDatePicker != null ? toDatePicker.getValue() : null;
        List<Sale> sales = new ArrayList<>();
        if (from == null || to == null) {
            LocalDate today = LocalDate.now();
            from = today;
            to = today;
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        sales = saleService.search(start, end, null, null);

        List<Sale> sold = sales.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (sold.isEmpty()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("تصدير المنتجات المباعة");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            File downloads = new File(home, "Downloads");
            if (downloads.exists() && downloads.isDirectory()) {
                chooser.setInitialDirectory(downloads);
            }
        }
        chooser.setInitialFileName("sold-products-" + LocalDate.now() + ".pdf");
        File file = chooser.showSaveDialog(
                ordersTable != null && ordersTable.getScene() != null ? ordersTable.getScene().getWindow() : null);
        if (file == null) {
            return;
        }

        Map<String, ProductSummary> summaries = new HashMap<>();
        for (Sale s : sold) {
            String name = s.getItemName() != null ? s.getItemName().trim() : "";
            if (name.isEmpty()) {
                name = "منتج غير معروف";
            }
            ProductSummary summary = summaries.computeIfAbsent(name, k -> new ProductSummary());
            summary.quantity += s.getQuantity() != null ? s.getQuantity() : 0;
            summary.total += s.getTotalAmount() != null ? s.getTotalAmount() : 0.0;
        }

        // Sort by total price descending
        Map<String, ProductSummary> sortedSummaries = summaries.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue().total, e1.getValue().total))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        try {
            exportSoldProductsPdf(file, sortedSummaries, from, to);
        } catch (Exception e) {
            System.out.println("Failed to export PDF: " + e.getMessage());
        }
    }

    private void exportSoldProductsPdf(File file, Map<String, ProductSummary> summaries, LocalDate from, LocalDate to)
            throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDType0Font font = loadArabicFont(doc);
            PDRectangle pageSize = PDRectangle.A4;
            float margin = 30f;
            float fontSize = 11f;
            float headerFontSize = 14f;
            float titleFontSize = 16f;
            float leading = 12f;
            float rowPadding = 5f;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream content = new PDPageContentStream(doc, page);
            content.setFont(font, titleFontSize);

            float y = pageSize.getHeight() - margin - 10f;
            float tableTopGap = 25f;

            // Header with larger font
            y = drawLine(content, font, titleFontSize, margin, pageSize.getWidth() - margin, y,
                    "تقرير المنتجات المباعة");
            content.setFont(font, fontSize);
            String rangeText = "الفترة: "
                    + (from != null ? from.toString() : "-")
                    + " إلى "
                    + (to != null ? to.toString() : "-");
            y = drawLine(content, font, fontSize, margin, pageSize.getWidth() - margin, y, rangeText);
            y = drawLine(content, font, fontSize, margin, pageSize.getWidth() - margin, y,
                    "التاريخ: " + LocalDate.now());

            y -= tableTopGap;

            float tableLeft = margin;
            float tableRight = pageSize.getWidth() - margin;
            float tableWidth = tableRight - tableLeft;

            // 3 columns: product (55%), qty (20%), total (25%)
            float colTotalWidth = tableWidth * 0.25f;
            float colQtyWidth = tableWidth * 0.20f;
            float colProductWidth = tableWidth - colTotalWidth - colQtyWidth;

            float colTotalRight = tableRight;
            float colQtyRight = colTotalRight - colTotalWidth;
            float colProductRight = colQtyRight - colQtyWidth;
            float colProductLeft = tableLeft;
            float colQtyLeft = colProductRight;
            float colTotalLeft = colQtyRight;

            // Table header
            float headerY = y;
            y = drawTableHeader(content, font, headerFontSize, tableLeft, tableRight, colProductLeft, colProductRight, 
                    colQtyLeft, colQtyRight, colTotalLeft, colTotalRight, y, leading, rowPadding);
            float tableTop = y;

            int totalQty = 0;
            double totalAmount = 0.0;

            for (Map.Entry<String, ProductSummary> entry : summaries.entrySet()) {
                ProductSummary s = entry.getValue();
                totalQty += s.quantity;
                totalAmount += s.total;

                String product = entry.getKey();
                List<String> productLines = wrapLine(product, font, fontSize, colProductWidth - 16f);
                int lines = Math.max(1, productLines.size());
                float rowHeight = (leading * lines) + (rowPadding * 2);

                if (y - rowHeight <= margin + 20) {
                    // Close vertical lines for current table
                    drawVLine(content, colProductLeft, headerY, y);
                    drawVLine(content, colProductRight, headerY, y);
                    drawVLine(content, colQtyRight, headerY, y);
                    drawVLine(content, colTotalRight, headerY, y);
                    
                    content.close();
                    page = new PDPage(pageSize);
                    doc.addPage(page);
                    content = new PDPageContentStream(doc, page);
                    content.setFont(font, fontSize);
                    y = pageSize.getHeight() - margin;

                    headerY = y;
                    y = drawTableHeader(content, font, headerFontSize, tableLeft, tableRight, colProductLeft, colProductRight,
                            colQtyLeft, colQtyRight, colTotalLeft, colTotalRight, y, leading, rowPadding);
                    tableTop = y;
                }

                float rowTop = y;
                float rowBottom = y - rowHeight;
                if (rowTop != tableTop) {
                    drawHLine(content, tableLeft, tableRight, rowTop);
                }
                // Calculate vertical center for single line or first line of multi-line
                float verticalOffset = rowHeight / 2f + fontSize / 2f;
                for (int i = 0; i < lines; i++) {
                    String line = i < productLines.size() ? productLines.get(i) : "";
                    float textY = rowTop - verticalOffset - (i * leading);
                    drawTextCentered(content, font, fontSize, shapeRtl(line), colProductLeft, colProductRight, textY);
                    if (i == 0) {
                        drawTextCentered(content, font, fontSize, shapeRtl(String.valueOf(s.quantity)),
                                colQtyLeft, colQtyRight, textY);
                        drawTextCentered(content, font, fontSize,
                                shapeRtl(String.format(Locale.ROOT, "%.2f", s.total)), colTotalLeft, colTotalRight, textY);
                    }
                }
                drawHLine(content, tableLeft, tableRight, rowBottom);
                y = rowBottom;
            }

            // Total row with bold effect
            float totalTop = y;
            float totalRowHeight = leading + (rowPadding * 2) + 2f;
            float totalBottom = y - totalRowHeight;
            drawHLine(content, tableLeft, tableRight, totalTop);
            float totalVerticalOffset = totalRowHeight / 2f + headerFontSize / 2f;
            float totalTextY = totalTop - totalVerticalOffset;
            
            // Draw total label - all centered
            drawTextCentered(content, font, headerFontSize, shapeRtl("الإجمالي"), colProductLeft, colProductRight, totalTextY);
            drawTextCentered(content, font, headerFontSize, shapeRtl(String.valueOf(totalQty)), 
                    colQtyLeft, colQtyRight, totalTextY);
            drawTextCentered(content, font, headerFontSize,
                    shapeRtl(String.format(Locale.ROOT, "%.2f", totalAmount)), colTotalLeft, colTotalRight, totalTextY);
            drawHLine(content, tableLeft, tableRight, totalBottom);

            // Draw vertical column lines for the entire table
            drawVLine(content, colProductLeft, headerY, totalBottom);
            drawVLine(content, colProductRight, headerY, totalBottom);
            drawVLine(content, colQtyRight, headerY, totalBottom);
            drawVLine(content, colTotalRight, headerY, totalBottom);

            content.close();
            doc.save(file);
        }
    }

    private float drawLine(PDPageContentStream content, PDType0Font font, float fontSize, float xLeft, float xRight,
            float y, String text) throws IOException {
        String shaped = shapeRtl(text);
        content.setFont(font, fontSize);
        content.beginText();
        content.newLineAtOffset(xLeft, y);
        content.showText(shaped);
        content.endText();
        return y - 20f;
    }

    private float drawTableHeader(PDPageContentStream content, PDType0Font font, float fontSize, float tableLeft,
            float tableRight, float colProductLeft, float colProductRight, float colQtyLeft, float colQtyRight, 
            float colTotalLeft, float colTotalRight, float y, float leading, float rowPadding)
            throws IOException {
        // Top border
        content.setLineWidth(1.5f);
        drawHLine(content, tableLeft, tableRight, y);
        content.setLineWidth(0.5f);
        
        float headerHeight = leading + (rowPadding * 2) + 2f;
        float textY = y - rowPadding - 3f;
        
        // Draw header text centered
        drawTextCentered(content, font, fontSize, shapeRtl("المنتج"), colProductLeft, colProductRight, textY);
        drawTextCentered(content, font, fontSize, shapeRtl("الكمية"), colQtyLeft, colQtyRight, textY);
        drawTextCentered(content, font, fontSize, shapeRtl("الإجمالي"), colTotalLeft, colTotalRight, textY);
        
        y -= headerHeight;
        content.setLineWidth(1.5f);
        drawHLine(content, tableLeft, tableRight, y);
        content.setLineWidth(0.5f);
        return y;
    }

    private void drawHLine(PDPageContentStream content, float xLeft, float xRight, float y) throws IOException {
        content.moveTo(xLeft, y);
        content.lineTo(xRight, y);
        content.stroke();
    }

    private void drawVLine(PDPageContentStream content, float x, float yTop, float yBottom) throws IOException {
        content.moveTo(x, yTop);
        content.lineTo(x, yBottom);
        content.stroke();
    }

    private void drawTextRight(PDPageContentStream content, PDType0Font font, float fontSize, String text, float xRight,
            float y) throws IOException {
        float width = font.getStringWidth(text) / 1000f * fontSize;
        content.beginText();
        content.newLineAtOffset(xRight - width, y);
        content.showText(text);
        content.endText();
    }

    private void drawTextCentered(PDPageContentStream content, PDType0Font font, float fontSize, String text,
            float xLeft, float xRight, float y) throws IOException {
        float width = font.getStringWidth(text) / 1000f * fontSize;
        float colWidth = xRight - xLeft;
        float x = xLeft + (colWidth - width) / 2f;
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private PDType0Font loadArabicFont(PDDocument doc) throws IOException {
        String windir = System.getenv("WINDIR");
        if (windir == null || windir.isBlank()) {
            windir = "C:\\\\Windows";
        }
        File tahoma = new File(windir + "\\\\Fonts\\\\tahoma.ttf");
        File arial = new File(windir + "\\\\Fonts\\\\arial.ttf");
        if (tahoma.exists()) {
            return PDType0Font.load(doc, tahoma);
        }
        if (arial.exists()) {
            return PDType0Font.load(doc, arial);
        }
        throw new IOException("No Arabic-capable font found.");
    }

    private String shapeRtl(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        try {
            ArabicShaping shaper = new ArabicShaping(ArabicShaping.LETTERS_SHAPE);
            String shaped = shaper.shape(text);
            Bidi bidi = new Bidi(shaped, Bidi.DIRECTION_RIGHT_TO_LEFT);
            return bidi.writeReordered(Bidi.DO_MIRRORING);
        } catch (ArabicShapingException e) {
            return text;
        } catch (Exception e) {
            return text;
        }
    }

    private List<String> wrapLine(String text, PDType0Font font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;
            if (width <= maxWidth || current.length() == 0) {
                current.setLength(0);
                current.append(candidate);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static class ProductSummary {
        int quantity = 0;
        double total = 0.0;
    }

    // VIEW MODEL
    public static class OrderViewModel {
        private String id;
        private String date;
        private String time;
        private String item;
        private int quantity;
        private double amount;
        private String status;
        private String worker;
        private String customer;
        private String notes;
        private String payment;
        private String deliveryMan;
        private boolean orderHeader;
        private List<Sale> orderSales;

        // Constructor for Single Item
        public OrderViewModel(Sale sale) {
            if (sale != null) {
                this.id = "";
                this.date = ""; // Child row doesn't need date
                this.time = ""; // Child row doesn't need time
                this.item = sale.getItemName();
                this.quantity = sale.getQuantity() != null ? sale.getQuantity() : 0;
                this.amount = sale.getTotalAmount() != null ? sale.getTotalAmount() : 0.0;

                String s = sale.getStatus() != null ? sale.getStatus().name() : "";
                if ("SOLD".equals(s))
                    this.status = "مباع";
                else if ("RETURNED".equals(s))
                    this.status = "مرتجع";
                else if ("DEFERRED".equals(s))
                    this.status = "آجل";
                else
                    this.status = s;

                this.worker = "";
                this.customer = "";
                this.notes = "";
                this.payment = "";
                this.deliveryMan = "";
                this.orderHeader = false;
                this.orderSales = java.util.Collections.emptyList();
            }
        }

        // Constructor for Order Header
        public OrderViewModel(List<Sale> sales) {
            if (sales != null && !sales.isEmpty()) {
                Sale first = sales.get(0);
                this.id = String.valueOf(first.getId()); // Use first ID as ref

                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm a");

                if (first.getTimestamp() != null) {
                    this.date = first.getTimestamp().format(df);
                    this.time = first.getTimestamp().format(tf);
                } else {
                    this.date = "";
                    this.time = "";
                }

                this.item = "طلب (" + sales.size() + " منتجات)";
                this.quantity = sales.stream().mapToInt(s -> s.getQuantity() != null ? s.getQuantity() : 0).sum();
                this.amount = sales.stream().mapToDouble(s -> s.getTotalAmount() != null ? s.getTotalAmount() : 0.0)
                        .sum();
                this.status = ""; // Aggregate status?

                if (first.getWorker() != null)
                    this.worker = first.getWorker().getFullName();
                else
                    this.worker = "-";

                if (first.getCustomer() != null)
                    this.customer = first.getCustomer().getCustomerName();
                else
                    this.customer = "عميل نقدي";

                String orderNotes = first.getNotes();
                this.payment = extractPaymentMethod(orderNotes);
                this.deliveryMan = extractDeliveryMan(orderNotes);
                this.notes = stripDeliveryFromNotes(orderNotes);
                this.orderHeader = true;
                this.orderSales = new java.util.ArrayList<>(sales);
            }
        }

        public String getId() {
            return id;
        }

        public String getDate() {
            return date;
        }

        public String getTime() {
            return time;
        }

        public String getItem() {
            return item;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getAmount() {
            return amount;
        }

        public String getStatus() {
            return status;
        }

        public String getWorker() {
            return worker;
        }

        public String getCustomer() {
            return customer;
        }

        public String getNotes() {
            return notes;
        }

        public String getPayment() {
            return payment;
        }

        public String getDeliveryMan() {
            return deliveryMan;
        }

        public boolean isOrderHeader() {
            return orderHeader;
        }

        public List<Sale> getOrderSales() {
            return orderSales;
        }

        private static String extractPaymentMethod(String notes) {
            if (notes == null || notes.isBlank()) {
                return "";
            }
            String base = notes.split("\\|", 2)[0].trim();
            String lower = base.toLowerCase(Locale.ROOT);
            if (lower.contains("payment")) {
                return base.replace("Payment", "").trim();
            }
            if (lower.startsWith("deferred")) {
                return "Deferred";
            }
            return base;
        }

        private static String extractDeliveryMan(String notes) {
            if (notes == null || notes.isBlank()) {
                return "";
            }
            int idx = notes.toLowerCase(Locale.ROOT).indexOf("delivery:");
            if (idx == -1) {
                return "";
            }
            String after = notes.substring(idx + "delivery:".length()).trim();
            int comma = after.indexOf(",");
            if (comma != -1) {
                after = after.substring(0, comma);
            }
            return after.trim();
        }

        private static String stripDeliveryFromNotes(String notes) {
            if (notes == null || notes.isBlank()) {
                return "";
            }
            String lower = notes.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf("delivery:");
            if (idx == -1) {
                return notes;
            }
            String before = notes.substring(0, idx).trim();
            if (before.endsWith(",")) {
                before = before.substring(0, before.length() - 1).trim();
            }
            if (before.endsWith("|")) {
                before = before.substring(0, before.length() - 1).trim();
            }
            return before;
        }
    }
}
