package com.library.pos.controller;

import com.library.pos.model.Product;
import com.library.pos.model.Sale;
import com.library.pos.model.User;
import com.library.pos.service.ProductService;
import com.library.pos.service.SaleService;
import com.library.pos.util.AutoRefreshUtil;
import com.library.pos.util.DialogUtil;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;

import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
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
    private Timeline autoRefreshTimeline;
    private static final int REFRESH_SECONDS_VISIBLE = 8;
    private static final int REFRESH_SECONDS_HIDDEN = 16;
    private LocalDateTime lastSaleTimestamp;
    private long lastSaleCount = -1;
    private java.time.LocalDateTime lastProductUpdatedAt;
    private long lastProductCount = -1;

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

        updateDataSignature();
        setupAutoRefresh();
    }

    private void setupAutoRefresh() {
        if (contentArea == null) {
            return;
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(REFRESH_SECONDS_VISIBLE), e -> {
            if (shouldRefresh() && stockTable != null) {
                refreshAnalytics();
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
        AutoRefreshUtil.bind(autoRefreshTimeline, contentArea,
                (double) REFRESH_SECONDS_VISIBLE / (double) REFRESH_SECONDS_HIDDEN);
    }

    private boolean shouldRefresh() {
        if (isUserEditing()) {
            return false;
        }
        return hasDataChanged();
    }

    private boolean isUserEditing() {
        return (searchField != null && searchField.isFocused())
                || (fromDatePicker != null && fromDatePicker.isFocused())
                || (toDatePicker != null && toDatePicker.isFocused())
                || (categoryFilter != null && categoryFilter.isFocused());
    }

    private boolean hasDataChanged() {
        LocalDateTime latestSale = saleService.getLatestSaleTimestamp();
        long saleCount = saleService.getTotalCount();
        java.time.LocalDateTime latestProduct = productService.getLatestUpdateTime();
        long productCount = productService.getTotalCount();

        boolean changed = !Objects.equals(latestSale, lastSaleTimestamp)
                || saleCount != lastSaleCount
                || !Objects.equals(latestProduct, lastProductUpdatedAt)
                || productCount != lastProductCount;
        if (changed) {
            lastSaleTimestamp = latestSale;
            lastSaleCount = saleCount;
            lastProductUpdatedAt = latestProduct;
            lastProductCount = productCount;
        }
        return changed;
    }

    private void updateDataSignature() {
        lastSaleTimestamp = saleService.getLatestSaleTimestamp();
        lastSaleCount = saleService.getTotalCount();
        lastProductUpdatedAt = productService.getLatestUpdateTime();
        lastProductCount = productService.getTotalCount();
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

    // Returns KPI
    @FXML
    private Label returnAmountLabel;
    @FXML
    private Label returnCountLabel;
    @FXML
    private BarChart<String, Number> topReturnsChart;

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
        updateReturnsAnalytics(start, end);
        updateCharts(sales);
        updateStockTable(products);
        updateDataSignature();
    }

    private void updateReturnsAnalytics(LocalDateTime start, LocalDateTime end) {
        // 1. Returns Amount
        Double totalReturns = saleService.getReturnsAmount(start, end);
        if (returnAmountLabel != null) {
            returnAmountLabel.setText(String.format("%.2f ج.م", totalReturns));
        }

        // 2. Returns Count
        Long countReturns = saleService.getReturnsCount(start, end);
        if (returnCountLabel != null) {
            returnCountLabel.setText(String.valueOf(countReturns));
        }

        // 3. Top Returned Products Chart
        if (topReturnsChart != null) {
            topReturnsChart.getData().clear();
            List<Object[]> topReturns = saleService.getTopReturnedProducts(start, end);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("المرتجعات");

            for (Object[] row : topReturns) {
                String product = (String) row[0];
                Number qty = (Number) row[1];
                series.getData().add(new XYChart.Data<>(product, qty));
            }
            topReturnsChart.getData().add(series);
        }
    }

    @FXML
    private Label salesTrendLabel;
    @FXML
    private Label productsBadge;

    private void updateKPICards(List<Sale> sales, List<Product> products) {
        // Total Sales (Filtered Range) - Exclude Returns from Revenue Calculation if
        // needed?
        // Usually Net Sales = Gross Sales - Returns.
        // The sales list contains both positive sales and negative returns.
        // So summing them up gives Net Sales automatically.
        double totalRevenue = sales.stream()
                .mapToDouble(s -> s.getTotalAmount() != null ? s.getTotalAmount() : 0)
                .sum();
        totalSalesLabel.setText(String.format("%.2f ج.م", totalRevenue));

        // Net Profit (real transaction-based calculation)
        double netProfit = saleService.calculateNetProfit(sales);
        netProfitLabel.setText(String.format("%.2f ج.م", netProfit));

        // Invoice Count (Only SOLD, exclude RETURNED for specific count?)
        // Or count all transactions? Let's count all for now.
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

        // Group by product name and sum quantities (Filter out returns for this chart)
        Map<String, Integer> productSales = sales.stream()
                .filter(s -> s.getItemName() != null && !s.getItemName().isEmpty())
                .filter(s -> s.getQuantity() > 0) // Only positive sales
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
    public void showSuppliers() {
        loadView("/fxml/suppliers.fxml");
    }

    @FXML
    public void showOrders() {
        loadView("/fxml/orders.fxml");
    }

    @FXML
    public void showDeferredPayments() {
        loadView("/fxml/deferred_payments.fxml");
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

            // Seamless transition
            Scene currentScene = contentArea.getScene();
            currentScene.setRoot(root);

            stage.setTitle(bundle.getString("cashier.title"));
            if (!stage.isFullScreen()) {
                stage.setFullScreen(true);
            }
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
            // Seamless transition
            Scene currentScene = contentArea.getScene();
            currentScene.setRoot(loader.load());

            stage.setTitle(bundle.getString("app.title"));
            if (!stage.isFullScreen()) {
                stage.setFullScreen(true);
            }
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
            DialogUtil.initOwner(alert, contentArea != null && contentArea.getScene() != null ? contentArea.getScene().getWindow() : null);
            alert.setTitle("خطأ");
            alert.setHeaderText("فشل تحميل الصفحة");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null && user != null)
            welcomeLabel.setText(user.getFullName());
    }

    public void setUsername(String username) {
        if (welcomeLabel != null)
            welcomeLabel.setText(username);
    }
}
