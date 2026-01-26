package com.library.pos.controller;

import com.library.pos.model.Product;
import com.library.pos.model.Sale;
import com.library.pos.model.User;
import com.library.pos.service.ProductService;
import com.library.pos.service.SaleService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DashboardController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private StackPane contentArea;

    // KPI Cards
    @FXML
    private Label totalSalesLabel;
    @FXML
    private Label netProfitLabel;
    @FXML
    private Label invoiceCountLabel;
    @FXML
    private Label lowStockLabel;

    // Filters
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private ComboBox<String> categoryFilter;

    // Charts
    @FXML
    private AreaChart<String, Number> salesChart;
    @FXML
    private PieChart topProductsChart;

    // Stock Table
    @FXML
    private TableView<Product> stockTable;
    @FXML
    private TableColumn<Product, String> stockProductCol;
    @FXML
    private TableColumn<Product, String> stockBarcodeCol;
    @FXML
    private TableColumn<Product, Integer> stockQtyCol;
    @FXML
    private TableColumn<Product, Double> stockPriceCol;
    @FXML
    private TableColumn<Product, String> stockStatusCol;
    @FXML
    private TextField searchField;

    private final ConfigurableApplicationContext applicationContext;
    private final SaleService saleService;
    private final ProductService productService;

    private User currentUser;
    private Parent defaultDashboardView;
    private ResourceBundle bundle;

    public DashboardController(ConfigurableApplicationContext applicationContext,
            SaleService saleService,
            ProductService productService) {
        this.applicationContext = applicationContext;
        this.saleService = saleService;
        this.productService = productService;
    }

    @FXML
    public void initialize() {
        bundle = ResourceBundle.getBundle("messages");

        // Save default view for home navigation
        if (contentArea != null && !contentArea.getChildren().isEmpty()) {
            defaultDashboardView = (Parent) contentArea.getChildren().get(0);
        }

        if (stockTable != null) {
            configureStockTable();
            setupFilters();
            // Initial Load: Last 30 Days
            fromDatePicker.setValue(LocalDate.now().minusDays(30));
            toDatePicker.setValue(LocalDate.now());
            refreshAnalytics();
        }
    }

    private void setupFilters() {
        // Populate Categories (Mock data for now, or distinct from DB)
        List<String> categories = new ArrayList<>();
        categories.add("الكل");
        categories.add("كتب");
        categories.add("أدوات مكتبية");
        categories.add("ألعاب");
        categoryFilter.setItems(FXCollections.observableArrayList(categories));
        categoryFilter.getSelectionModel().selectFirst();
    }

    @FXML
    public void refreshAnalytics() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        if (from == null)
            from = LocalDate.now().minusDays(30);
        if (to == null)
            to = LocalDate.now();

        LocalDateTime start = LocalDateTime.of(from, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(to, LocalTime.MAX);

        // Fetch Data
        List<Sale> sales = saleService.findByRange(start, end);
        List<Product> products = productService.getAll();

        updateKPICards(sales, products);
        updateCharts(sales);
        updateStockTable(products);
    }

    @FXML
    private Label salesTrendLabel;
    @FXML
    private Label productsBadge;

    private void updateKPICards(List<Sale> sales, List<Product> products) {
        // Total Sales (Filtered Range)
        double totalRevenue = sales.stream()
                .mapToDouble(s -> s.getTotalAmount() != null ? s.getTotalAmount() : 0)
                .sum();
        totalSalesLabel.setText(String.format("%.2f ج.م", totalRevenue));

        // Net Profit (Estimated 25%)
        double estimatedProfit = totalRevenue * 0.25;
        netProfitLabel.setText(String.format("%.2f ج.م", estimatedProfit));

        // Invoice Count
        invoiceCountLabel.setText(String.valueOf(sales.size()));

        // Low Stock
        long lowStockCount = products.stream()
                .filter(p -> p.getQuantity() <= p.getMinStock())
                .count();
        lowStockLabel.setText(String.valueOf(lowStockCount));

        if (productsBadge != null) {
            productsBadge.setVisible(lowStockCount > 0);
        }

        // Sales Trend (Current Month vs Last Month)
        // Sales Trend (Current Month vs Last Month)
        calculateSalesTrend();
    }

    private void calculateSalesTrend() {
        if (salesTrendLabel == null)
            return;

        try {
            LocalDate now = LocalDate.now();
            LocalDateTime startCurrent = now.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endCurrent = now.atTime(LocalTime.MAX);

            LocalDateTime startLast = now.minusMonths(1).withDayOfMonth(1).atStartOfDay();
            LocalDateTime endLast = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth())
                    .atTime(LocalTime.MAX);

            double currentMonthSales = saleService.findByRange(startCurrent, endCurrent).stream()
                    .mapToDouble(s -> s.getTotalAmount() != null ? s.getTotalAmount() : 0).sum();

            double lastMonthSales = saleService.findByRange(startLast, endLast).stream()
                    .mapToDouble(s -> s.getTotalAmount() != null ? s.getTotalAmount() : 0).sum();

            if (lastMonthSales == 0) {
                salesTrendLabel.setText("N/A عن الشهر الماضي");
                salesTrendLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");
            } else {
                double growth = ((currentMonthSales - lastMonthSales) / lastMonthSales) * 100;
                String sign = growth >= 0 ? "+" : "";
                String color = growth >= 0 ? "-color-success" : "-color-danger";

                salesTrendLabel.setText(String.format("%s %.1f%% عن الشهر الماضي", sign, growth));
                salesTrendLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            }
        } catch (Exception e) {
            System.err.println("Error calculating sales trend: " + e.getMessage());
            e.printStackTrace();
            salesTrendLabel.setText("خطأ");
        }
    }

    private void updateCharts(List<Sale> sales) {
        // --- 1. Area Chart: Sales Over Time ---
        salesChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("المبيعات");

        // Group by Date
        Map<LocalDate, Double> salesByDate = sales.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getTimestamp().toLocalDate(),
                        Collectors.summingDouble(Sale::getTotalAmount)));

        // Sort by Date
        salesByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    series.getData().add(new XYChart.Data<>(entry.getKey().toString(), entry.getValue()));
                });

        salesChart.getData().add(series);

        // --- 2. Pie Chart: Top Products by Sales Quantity ---
        topProductsChart.getData().clear();

        // Group by product name and sum quantities
        Map<String, Integer> productSales = sales.stream()
                .filter(s -> s.getItemName() != null && !s.getItemName().isEmpty())
                .collect(Collectors.groupingBy(
                        Sale::getItemName,
                        Collectors.summingInt(s -> s.getQuantity() != null ? s.getQuantity() : 0)));

        // Sort by quantity and take top 5
        productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    topProductsChart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
                });
    }

    private void updateStockTable(List<Product> products) {
        stockTable.setItems(FXCollections.observableArrayList(products));
    }

    // --- Stock Table Config ---
    private void configureStockTable() {
        stockProductCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        stockBarcodeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBarcode()));
        stockQtyCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        stockPriceCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getSellPrice()).asObject());

        stockPriceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%.2f", item));
            }
        });

        stockStatusCol.setCellValueFactory(data -> {
            int qty = data.getValue().getQuantity();
            int min = data.getValue().getMinStock();
            return new SimpleStringProperty(qty <= 0 ? "نفذ" : (qty <= min ? "منخفض" : "متوفر"));
        });
    }

    // --- Search ---
    @FXML
    private void handleSearch() {
        String term = searchField.getText();
        if (term == null || term.isBlank()) {
            refreshAnalytics(); // Reset
            return;
        }
        List<Product> results = productService.searchByBarcodeOrName(term.trim());
        stockTable.setItems(FXCollections.observableArrayList(results));
    }

    // --- Navigation ---
    @FXML
    public void showDashboard() {
        if (defaultDashboardView != null) {
            contentArea.getChildren().setAll(defaultDashboardView);
            refreshAnalytics();
        }
    }

    @FXML
    public void showWorkers() {
        loadView("/fxml/workers.fxml");
    }

    @FXML
    public void showProducts() {
        loadView("/fxml/products.fxml");
    }

    @FXML
    public void showCustomers() {
        loadView("/fxml/customers.fxml");
    }

    @FXML
    public void showOrders() {
        loadView("/fxml/orders.fxml");
    }

    @FXML
    public void openCashier() {
        try {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cashier.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            loader.setResources(bundle);
            Parent root = loader.load();
            CashierController controller = loader.getController();
            if (currentUser != null)
                controller.setUser(currentUser);
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(bundle.getString("cashier.title"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            loader.setResources(bundle);
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle(bundle.getString("app.title"));
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            loader.setResources(bundle);
            contentArea.getChildren().setAll((Parent) loader.load());
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("خطأ");
            alert.setHeaderText("فشل تحميل الصفحة");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null && user != null)
            welcomeLabel.setText("👤 " + user.getFullName());
    }

    public void setUsername(String username) {
        if (welcomeLabel != null)
            welcomeLabel.setText("👤 " + username);
    }
}
