package com.library.pos.controller;

import com.library.pos.model.Product;
import com.library.pos.model.Sale;
import com.library.pos.model.SaleStatus;
import com.library.pos.model.User;
import com.library.pos.service.ProductService;
import com.library.pos.service.SaleService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

@Component
public class CashierController {

    // Header
    @FXML
    private Label workerNameLabel;
    @FXML
    private Label dateTimeLabel;

    // Barcode input
    @FXML
    private TextField barcodeField;

    // Product display
    @FXML
    private VBox productCard;
    @FXML
    private Label productNameLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label stockLabel;
    @FXML
    private Label barcodeDisplayLabel;

    // Quantity controls
    @FXML
    private TextField qtyField;
    @FXML
    private Button qtyMinusBtn;
    @FXML
    private Button qtyPlusBtn;
    @FXML
    private Button addButton;

    // Cart
    @FXML
    private TableView<CartItem> cartTable;
    @FXML
    private TableColumn<CartItem, String> productCol;
    @FXML
    private TableColumn<CartItem, Integer> qtyCol;
    @FXML
    private TableColumn<CartItem, Double> priceCol;
    @FXML
    private TableColumn<CartItem, Double> totalCol;
    @FXML
    private TableColumn<CartItem, Void> actionCol;

    // Summary
    @FXML
    private Label itemCountLabel;
    @FXML
    private Label grandTotalLabel;

    private final ProductService productService;
    private final SaleService saleService;
    private final ApplicationContext applicationContext;
    private final com.library.pos.repository.WorkSessionRepository sessionRepository;

    private User currentUser;
    private Product selectedProduct;
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private ResourceBundle bundle;
    private com.library.pos.model.WorkSession currentSession;

    public CashierController(ProductService productService, SaleService saleService,
            ApplicationContext applicationContext, com.library.pos.repository.WorkSessionRepository sessionRepository) {
        this.productService = productService;
        this.saleService = saleService;
        this.applicationContext = applicationContext;
        this.sessionRepository = sessionRepository;
    }

    @FXML
    public void initialize() {
        bundle = ResourceBundle.getBundle("messages");

        cartTable.setItems(cartItems);
        setupColumns();
        resetSelection();
        startClock();

        // Auto-focus barcode field
        javafx.application.Platform.runLater(() -> barcodeField.requestFocus());

        // Setup keyboard shortcuts
        setupKeyboardShortcuts();
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) {
            workerNameLabel.setText("👤 " + user.getFullName());

            // Start Session
            currentSession = new com.library.pos.model.WorkSession(user, LocalDateTime.now());
            sessionRepository.save(currentSession);
        }
    }

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd  HH:mm:ss");
            if (dateTimeLabel != null) {
                dateTimeLabel.setText(now.format(formatter));
            }
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void setupKeyboardShortcuts() {
        // Will be set up when scene is available
        javafx.application.Platform.runLater(() -> {
            if (barcodeField.getScene() != null) {
                barcodeField.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    // F1-F6 for quick products
                    if (event.getCode().isFunctionKey()) {
                        handleFunctionKey(event.getCode());
                        event.consume();
                    }
                    // Enter to add to cart when product is selected
                    if (event.getCode() == KeyCode.ESCAPE) {
                        resetSelection();
                        barcodeField.requestFocus();
                        event.consume();
                    }
                });
            }
        });
    }

    private void handleFunctionKey(KeyCode code) {
        // Quick product shortcuts (would be configured by user)
        String barcode = null;
        switch (code) {
            case F1:
                barcode = "1001";
                break;
            case F2:
                barcode = "1002";
                break;
            case F3:
                barcode = "1003";
                break;
            case F4:
                barcode = "1004";
                break;
            case F5:
                barcode = "1005";
                break;
            case F6:
                barcode = "1006";
                break;
            default:
                break;
        }
        if (barcode != null) {
            searchAndSelectProduct(barcode);
        }
    }

    private void setupColumns() {
        productCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct().getName()));
        qtyCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        priceCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getPrice()).asObject());
        totalCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getTotal()).asObject());

        // Format price columns
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%.2f", item));
            }
        });

        totalCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%.2f", item));
                if (!empty && item != null) {
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981;");
                }
            }
        });

        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✕");
            {
                btn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-min-width: 30; -fx-min-height: 30; -fx-background-radius: 15; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    cartItems.remove(item);
                    updateSummary();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    // Barcode scan handler (triggered on Enter)
    @FXML
    private void handleBarcodeEnter() {
        String term = barcodeField.getText();
        if (term == null || term.isBlank())
            return;
        searchAndSelectProduct(term.trim());
    }

    @FXML
    private void handleSearch() {
        handleBarcodeEnter();
    }

    private void searchAndSelectProduct(String term) {
        var results = productService.searchByBarcodeOrName(term);
        if (results.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.notfound"));
            resetSelection();
            barcodeField.selectAll();
        } else {
            selectProduct(results.get(0));
            barcodeField.clear();
        }
    }

    private void selectProduct(Product p) {
        this.selectedProduct = p;
        productNameLabel.setText(p.getName());
        priceLabel.setText(String.format("%.2f", p.getSellPrice()));
        stockLabel.setText(String.valueOf(p.getQuantity()));
        barcodeDisplayLabel.setText(p.getBarcode());

        // Update stock label color based on quantity
        if (p.getQuantity() <= 0) {
            stockLabel.setStyle("-fx-text-fill: #ef4444;");
        } else if (p.getQuantity() <= p.getMinStock()) {
            stockLabel.setStyle("-fx-text-fill: #f59e0b;");
        } else {
            stockLabel.setStyle("-fx-text-fill: #94a3b8;");
        }

        addButton.setDisable(false);
        qtyField.setText("1");
        qtyField.requestFocus();
        qtyField.selectAll();
    }

    private void resetSelection() {
        selectedProduct = null;
        productNameLabel.setText("-");
        priceLabel.setText("0.00");
        stockLabel.setText("-");
        stockLabel.setStyle("-fx-text-fill: #94a3b8;");
        barcodeDisplayLabel.setText("-");
        addButton.setDisable(true);
        qtyField.setText("1");
    }

    @FXML
    private void handleQtyMinus() {
        try {
            int qty = Integer.parseInt(qtyField.getText().trim());
            if (qty > 1) {
                qtyField.setText(String.valueOf(qty - 1));
            }
        } catch (NumberFormatException e) {
            qtyField.setText("1");
        }
    }

    @FXML
    private void handleQtyPlus() {
        try {
            int qty = Integer.parseInt(qtyField.getText().trim());
            if (selectedProduct != null && qty < selectedProduct.getQuantity()) {
                qtyField.setText(String.valueOf(qty + 1));
            }
        } catch (NumberFormatException e) {
            qtyField.setText("1");
        }
    }

    @FXML
    private void handleAddToCart() {
        if (selectedProduct == null)
            return;

        try {
            int qty = Integer.parseInt(qtyField.getText().trim());
            if (qty <= 0) {
                showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.qty"));
                return;
            }

            // Check stock
            if (qty > selectedProduct.getQuantity()) {
                showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.stock"));
                return;
            }

            // Add to cart or update existing
            Optional<CartItem> existing = cartItems.stream()
                    .filter(i -> i.getProduct().getId().equals(selectedProduct.getId()))
                    .findFirst();

            if (existing.isPresent()) {
                int newQty = existing.get().getQuantity() + qty;
                if (newQty > selectedProduct.getQuantity()) {
                    showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.stock"));
                    return;
                }
                existing.get().setQuantity(newQty);
                cartTable.refresh();
            } else {
                cartItems.add(new CartItem(selectedProduct, qty));
            }

            updateSummary();
            resetSelection();
            barcodeField.requestFocus();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.qty"));
        }
    }

    @FXML
    private void handleClearCart() {
        if (cartItems.isEmpty())
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(bundle.getString("cashier.confirm"));
        confirm.setHeaderText(bundle.getString("cashier.clear.confirm"));
        confirm.setContentText(bundle.getString("cashier.clear.message"));

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            cartItems.clear();
            updateSummary();
        }
    }

    private void updateSummary() {
        int itemCount = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        double total = cartItems.stream().mapToDouble(CartItem::getTotal).sum();

        itemCountLabel.setText(String.valueOf(itemCount));
        grandTotalLabel.setText(String.format("%.2f", total));
    }

    @FXML
    private void handleCashPayment() {
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.empty"));
            return;
        }
        showCashPaymentDialog();
    }

    @FXML
    private void handleDeferredPayment() {
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.empty"));
            return;
        }
        showDeferredPaymentDialog();
    }

    private void showCashPaymentDialog() {
        double total = cartItems.stream().mapToDouble(CartItem::getTotal).sum();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle(bundle.getString("cashier.cash"));

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12;");

        // Header
        Label header = new Label("💵 " + bundle.getString("cashier.cash"));
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Total
        HBox totalBox = new HBox(10);
        totalBox.setAlignment(Pos.CENTER_LEFT);
        totalBox.setStyle("-fx-background-color: #334155; -fx-padding: 16; -fx-background-radius: 8;");
        Label totalLabel = new Label(bundle.getString("cashier.total") + ":");
        totalLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #94a3b8;");
        Label totalValue = new Label(String.format("%.2f ج.م", total));
        totalValue.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        totalBox.getChildren().addAll(totalLabel, spacer, totalValue);

        // Cash received
        VBox cashBox = new VBox(8);
        Label cashLabel = new Label(bundle.getString("cashier.received") + ":");
        cashLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
        TextField cashField = new TextField();
        cashField.setPromptText("0.00");
        cashField.setStyle("-fx-font-size: 24px; -fx-background-color: #334155; -fx-text-fill: white; " +
                "-fx-border-color: #475569; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-padding: 12; -fx-alignment: center;");
        cashBox.getChildren().addAll(cashLabel, cashField);

        // Change
        HBox changeBox = new HBox(10);
        changeBox.setAlignment(Pos.CENTER_LEFT);
        changeBox.setStyle("-fx-background-color: rgba(16,185,129,0.1); -fx-border-color: #10b981; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16;");
        Label changeLabel = new Label(bundle.getString("cashier.change") + ":");
        changeLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #10b981;");
        Label changeValue = new Label("0.00 ج.م");
        changeValue.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        changeBox.getChildren().addAll(changeLabel, spacer2, changeValue);

        // Update change on input
        cashField.textProperty().addListener((obs, old, newVal) -> {
            try {
                double received = Double.parseDouble(newVal);
                double change = received - total;
                changeValue.setText(String.format("%.2f ج.م", Math.max(0, change)));
                changeValue.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; " +
                        "-fx-text-fill: " + (change >= 0 ? "#10b981" : "#ef4444") + ";");
            } catch (NumberFormatException e) {
                changeValue.setText("0.00 ج.م");
            }
        });

        // Buttons
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button(bundle.getString("cashier.cancel"));
        cancelBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 12 24; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button("✓ " + bundle.getString("cashier.confirm"));
        confirmBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 12 32; -fx-background-radius: 8; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> {
            try {
                double received = Double.parseDouble(cashField.getText());
                if (received < total) {
                    showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.insufficient"));
                    return;
                }
                completeSale("CASH", null);
                dialog.close();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.amount"));
            }
        });
        buttons.getChildren().addAll(cancelBtn, confirmBtn);

        root.getChildren().addAll(header, totalBox, cashBox, changeBox, buttons);

        Scene scene = new Scene(root, 400, 380);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showDeferredPaymentDialog() {
        double total = cartItems.stream().mapToDouble(CartItem::getTotal).sum();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle(bundle.getString("cashier.deferred"));

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12;");

        // Header
        Label header = new Label("💳 " + bundle.getString("cashier.deferred"));
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Total
        HBox totalBox = new HBox(10);
        totalBox.setAlignment(Pos.CENTER_LEFT);
        totalBox.setStyle("-fx-background-color: #334155; -fx-padding: 16; -fx-background-radius: 8;");
        Label totalLabel = new Label(bundle.getString("cashier.total") + ":");
        totalLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #94a3b8;");
        Label totalValue = new Label(String.format("%.2f ج.م", total));
        totalValue.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        totalBox.getChildren().addAll(totalLabel, spacer, totalValue);

        // Customer name
        VBox customerBox = new VBox(8);
        Label customerLabel = new Label(bundle.getString("cashier.customer") + ":");
        customerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
        TextField customerField = new TextField();
        customerField.setPromptText(bundle.getString("cashier.customer.placeholder"));
        customerField.setStyle("-fx-font-size: 18px; -fx-background-color: #334155; -fx-text-fill: white; " +
                "-fx-border-color: #475569; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-padding: 12;");
        customerBox.getChildren().addAll(customerLabel, customerField);

        // Phone
        VBox phoneBox = new VBox(8);
        Label phoneLabel = new Label(
                bundle.getString("cashier.phone") + " (" + bundle.getString("cashier.optional") + "):");
        phoneLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
        TextField phoneField = new TextField();
        phoneField.setPromptText("01xxxxxxxxx");
        phoneField.setStyle("-fx-font-size: 18px; -fx-background-color: #334155; -fx-text-fill: white; " +
                "-fx-border-color: #475569; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-padding: 12;");
        phoneBox.getChildren().addAll(phoneLabel, phoneField);

        // Buttons
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button(bundle.getString("cashier.cancel"));
        cancelBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 12 24; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button("✓ " + bundle.getString("cashier.register.deferred"));
        confirmBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: #1e293b; -fx-font-weight: bold; " +
                "-fx-padding: 12 32; -fx-background-radius: 8; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> {
            String customer = customerField.getText().trim();
            if (customer.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.customer"));
                return;
            }
            completeSale("DEFERRED", customer);
            dialog.close();
        });
        buttons.getChildren().addAll(cancelBtn, confirmBtn);

        root.getChildren().addAll(header, totalBox, customerBox, phoneBox, buttons);

        Scene scene = new Scene(root, 400, 380);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void completeSale(String paymentType, String customerName) {
        // Process sales
        for (CartItem item : cartItems) {
            Sale sale = new Sale(
                    LocalDateTime.now(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getTotal(),
                    SaleStatus.SOLD,
                    currentUser,
                    paymentType + (customerName != null ? " - " + customerName : ""));
            sale.setProduct(item.getProduct());
            saleService.save(sale);
        }

        cartItems.clear();
        updateSummary();
        resetSelection();
        barcodeField.requestFocus();

        showAlert(Alert.AlertType.INFORMATION, bundle.getString("cashier.success"));
    }

    @FXML
    private void handleQuickBtn() {
        // Placeholder for quick buttons - would be configured
    }

    @FXML
    private void handleLogout() {
        // End Session
        if (currentSession != null) {
            currentSession.setEndTime(LocalDateTime.now());
            long minutes = java.time.Duration.between(currentSession.getStartTime(), currentSession.getEndTime())
                    .toMinutes();
            currentSession.setDurationMinutes(minutes);
            sessionRepository.save(currentSession);
        }

        try {
            Stage stage = (Stage) barcodeField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            loader.setResources(bundle);
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle(bundle.getString("app.title"));
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setContentText(msg);
        alert.show();
    }

    public static class CartItem {
        private final Product product;
        private int quantity;
        private final double price;

        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
            this.price = product.getSellPrice();
        }

        public double getTotal() {
            return price * quantity;
        }

        public Product getProduct() {
            return product;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getPrice() {
            return price;
        }

        public void setQuantity(int q) {
            this.quantity = q;
        }
    }
}
