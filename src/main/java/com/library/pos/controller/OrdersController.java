package com.library.pos.controller;

import com.library.pos.model.Sale;
import com.library.pos.service.SaleService;
import com.library.pos.util.AutoRefreshUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
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

@Component
public class OrdersController {

    @FXML
    private TreeTableView<OrderViewModel> ordersTable;
    @FXML
    private TreeTableColumn<OrderViewModel, String> idCol;
    @FXML
    private TreeTableColumn<OrderViewModel, String> timeCol;
    @FXML
    private TreeTableColumn<OrderViewModel, String> itemCol;
    @FXML
    private TreeTableColumn<OrderViewModel, Integer> quantityCol;
    @FXML
    private TreeTableColumn<OrderViewModel, String> notesCol;

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

    // Payment Method Summary Labels
    @FXML
    private javafx.scene.control.Label cashTotalLabel;
    @FXML
    private javafx.scene.control.Label instapayTotalLabel;
    @FXML
    private javafx.scene.control.Label visaTotalLabel;
    @FXML
    private javafx.scene.control.Label vodafoneTotalLabel;

    private final SaleService saleService;
    private final com.library.pos.service.UserService userService;
    private Timeline autoRefreshTimeline;
    private static final int AUTO_REFRESH_SECONDS = 3;
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
        notesCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getNotes()));

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

        // Default: This month
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now());

        // Initial Load
        handleFilter();
        setupAutoRefresh();
    }

    private void setupAutoRefresh() {
        if (ordersTable == null) {
            return;
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(AUTO_REFRESH_SECONDS), e -> handleFilter()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
        AutoRefreshUtil.bind(autoRefreshTimeline, ordersTable, 0.5);
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
            updatePaymentMethodTotals(sales);
        }

        // Populate Tree
        populateTree(sales);
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

    @FXML
    public void loadAll() {
        List<Sale> sales = saleService.getAll();
        updatePaymentMethodTotals(sales);
        populateTree(sales);
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
                this.notes = sale.getNotes();
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

                // Aggregate notes or take first?
                this.notes = sales.stream()
                        .map(Sale::getNotes)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.joining(", "));
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
    }
}
