package com.library.pos.controller;

import com.library.pos.model.Sale;
import com.library.pos.service.SaleService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SalesReportController {

    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private TextField searchField;

    @FXML
    private TableView<ProductSalesRow> salesTable;
    @FXML
    private TableColumn<ProductSalesRow, String> productNameCol;
    @FXML
    private TableColumn<ProductSalesRow, Integer> soldQtyCol;
    @FXML
    private TableColumn<ProductSalesRow, Integer> txCountCol;
    @FXML
    private TableColumn<ProductSalesRow, Double> totalAmountCol;

    @FXML
    private Label totalSoldLabel;
    @FXML
    private Label uniqueProductsLabel;

    private final SaleService saleService;

    public SalesReportController(SaleService saleService) {
        this.saleService = saleService;
    }

    @FXML
    public void initialize() {
        configureTable();
        fromDatePicker.setValue(LocalDate.now().minusDays(30));
        toDatePicker.setValue(LocalDate.now());
        applyFilters();
    }

    private void configureTable() {
        productNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        soldQtyCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getSoldQuantity()).asObject());
        txCountCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTransactionCount()).asObject());
        totalAmountCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getTotalAmount()).asObject());

        totalAmountCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "" : String.format("%.2f ج.م", value));
            }
        });
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleFilter() {
        applyFilters();
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        fromDatePicker.setValue(LocalDate.now().minusDays(30));
        toDatePicker.setValue(LocalDate.now());
        applyFilters();
    }

    private void applyFilters() {
        LocalDate from = fromDatePicker.getValue() != null ? fromDatePicker.getValue() : LocalDate.now().minusDays(30);
        LocalDate to = toDatePicker.getValue() != null ? toDatePicker.getValue() : LocalDate.now();

        if (from.isAfter(to)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("تنبيه");
            alert.setHeaderText("تاريخ غير صحيح");
            alert.setContentText("تاريخ البداية يجب أن يكون قبل أو يساوي تاريخ النهاية.");
            alert.showAndWait();
            return;
        }

        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        List<Sale> sales = saleService.findByRange(start, end);

        Map<String, Stats> grouped = new LinkedHashMap<>();
        for (Sale sale : sales) {
            if (sale == null || sale.getItemName() == null || sale.getItemName().isBlank()) {
                continue;
            }
            if (sale.isDeliveryCharge()) {
                continue;
            }
            Integer saleQty = sale.getQuantity();
            int qty = saleQty != null ? saleQty : 0;
            if (qty <= 0) {
                continue;
            }

            String productName = sale.getItemName().trim();
            if (!search.isEmpty() && !productName.toLowerCase(Locale.ROOT).contains(search)) {
                continue;
            }

            Stats stats = grouped.computeIfAbsent(productName, key -> new Stats());
            stats.soldQty += qty;
            stats.txCount += 1;
            Double amount = sale.getTotalAmount();
            stats.totalAmount += amount != null ? amount : 0.0;
        }

        List<ProductSalesRow> rows = grouped.entrySet().stream()
                .map(entry -> new ProductSalesRow(entry.getKey(), entry.getValue().soldQty, entry.getValue().txCount,
                        entry.getValue().totalAmount))
                .sorted(Comparator.comparingInt(ProductSalesRow::getSoldQuantity).reversed()
                        .thenComparing(ProductSalesRow::getProductName))
                .collect(Collectors.toList());

        salesTable.setItems(FXCollections.observableArrayList(rows));

        int totalSold = rows.stream().mapToInt(ProductSalesRow::getSoldQuantity).sum();
        totalSoldLabel.setText(String.valueOf(totalSold));
        uniqueProductsLabel.setText(String.valueOf(rows.size()));
    }

    private static class Stats {
        private int soldQty;
        private int txCount;
        private double totalAmount;
    }

    public static class ProductSalesRow {
        private final String productName;
        private final int soldQuantity;
        private final int transactionCount;
        private final double totalAmount;

        public ProductSalesRow(String productName, int soldQuantity, int transactionCount, double totalAmount) {
            this.productName = productName;
            this.soldQuantity = soldQuantity;
            this.transactionCount = transactionCount;
            this.totalAmount = totalAmount;
        }

        public String getProductName() {
            return productName;
        }

        public int getSoldQuantity() {
            return soldQuantity;
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public double getTotalAmount() {
            return totalAmount;
        }
    }
}
