package com.library.pos.controller;

import com.library.pos.model.Customer;
import com.library.pos.model.OpenOrder;
import com.library.pos.model.OpenOrderItem;
import com.library.pos.model.OpenOrderStatus;
import com.library.pos.model.PaymentMethod;
import com.library.pos.model.Product;
import com.library.pos.model.Sale;
import com.library.pos.model.Supplier;
import com.library.pos.model.SupplierPayment;
import com.library.pos.model.User;
import com.library.pos.service.CustomerService;
import com.library.pos.service.OpenOrderService;
import com.library.pos.service.ProductService;
import com.library.pos.service.SaleService;
import com.library.pos.service.SupplierService;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
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
import com.library.pos.util.AutoRefreshUtil;
import com.library.pos.util.StageUtil;
import com.library.pos.util.DialogUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.List;
import java.util.ResourceBundle;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.Properties;
import java.io.InputStream;
import javafx.event.ActionEvent;

@Component
public class CashierController {

    // Root overlay container
    @FXML
    private StackPane cashierRootStack;

    // Header
    @FXML
    private Label workerNameLabel;
    @FXML
    private Label dateTimeLabel;
    @FXML
    private Button backButton;
    @FXML
    private Label dailyCashLabel;
    @FXML
    private Label dailySupplierPaymentsLabel;

    // Barcode input
    @FXML
    private TextField barcodeField;

    @FXML
    private ListView<Product> suggestionList;

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

    // Payment Method
    @FXML
    private ComboBox<String> paymentMethodCombo;

    // Customer & Orders (Right Panel)
    @FXML
    private TextField customerSearchField;
    @FXML
    private ListView<Customer> customerSearchList;
    @FXML
    private Label customerAddressLabel;
    @FXML
    private ComboBox<User> deliveryManCombo;
    @FXML
    private Label deliveryManSummaryLabel;
    @FXML
    private ListView<OpenOrder> openDeliveryOrdersList;
    @FXML
    private ListView<OpenOrder> openStoreOrdersList;
    @FXML
    private Button newOrderButton;
    @FXML
    private Button saveOrderButton;
    @FXML
    private Button deleteOrderButton;

    private final ProductService productService;
    private final SaleService saleService;
    private final CustomerService customerService;
    private final OpenOrderService openOrderService;
    private final ApplicationContext applicationContext;
    private final com.library.pos.repository.WorkSessionRepository sessionRepository;
    private final com.library.pos.service.UserService userService;
    private final SupplierService supplierService;

    private User currentUser;
    private Customer selectedCustomer;
    private OpenOrder activeOrder;
    private Product selectedProduct;
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private final ObservableList<Product> suggestionItems = FXCollections.observableArrayList();
    private final ObservableList<Customer> customerSearchResults = FXCollections.observableArrayList();
    private final ObservableList<OpenOrder> openDeliveryOrders = FXCollections.observableArrayList();
    private final ObservableList<OpenOrder> openStoreOrders = FXCollections.observableArrayList();
    private ResourceBundle bundle;
    private com.library.pos.model.WorkSession currentSession;
    private final Properties quickKeys = new Properties();
    private Timeline autoRefreshTimeline;
    private static final int REFRESH_SECONDS_VISIBLE = 3;
    private static final int REFRESH_SECONDS_HIDDEN = 6;
    private java.time.LocalDateTime lastSaleTimestamp;
    private long lastSaleCount = -1;

    public CashierController(ProductService productService, SaleService saleService,
            CustomerService customerService, OpenOrderService openOrderService,
            ApplicationContext applicationContext, com.library.pos.repository.WorkSessionRepository sessionRepository,
            com.library.pos.service.UserService userService, SupplierService supplierService) {
        this.productService = productService;
        this.saleService = saleService;
        this.customerService = customerService;
        this.openOrderService = openOrderService;
        this.applicationContext = applicationContext;
        this.sessionRepository = sessionRepository;
        this.userService = userService;
        this.supplierService = supplierService;
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

        // Setup cart table row click for quick quantity edit
        setupCartTableClickHandler();
        setupAutoRefresh();
        setupSuggestions();

        // Initialize payment methods
        // Initialize payment methods
        paymentMethodCombo.setItems(FXCollections.observableArrayList(
                "Cash",
                "InstaPay",
                "Visa",
                "Vodafone Cash"));

        paymentMethodCombo.setCellFactory(param -> new PaymentMethodListCell());
        paymentMethodCombo.setButtonCell(new PaymentMethodListCell());

        paymentMethodCombo.getSelectionModel().selectFirst();

        loadQuickKeys();
        setupCustomerSearch();
        setupOpenOrdersList();
        loadDeliveryMen();
        loadOpenOrders();
        updateOrderButtons();
        updateDeliveryManSummary();
    }

    private void setupAutoRefresh() {
        if (barcodeField == null) {
            return;
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(REFRESH_SECONDS_VISIBLE), e -> {
            if (shouldRefresh()) {
                updateDailyCash();
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
        AutoRefreshUtil.bind(autoRefreshTimeline, barcodeField,
                (double) REFRESH_SECONDS_VISIBLE / (double) REFRESH_SECONDS_HIDDEN);
        updateDataSignature();
    }

    private boolean shouldRefresh() {
        if (isUserEditing()) {
            return false;
        }
        return hasDataChanged();
    }

    private boolean isUserEditing() {
        return barcodeField != null && barcodeField.isFocused();
    }

    private boolean hasDataChanged() {
        java.time.LocalDateTime latest = saleService.getLatestSaleTimestamp();
        long count = saleService.getTotalCount();
        boolean changed = !java.util.Objects.equals(latest, lastSaleTimestamp) || count != lastSaleCount;
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

    private void loadQuickKeys() {
        try (InputStream input = getClass().getResourceAsStream("/quick_keys.properties")) {
            if (input != null) {
                quickKeys.load(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) {
            workerNameLabel.setText(user.getFullName());

            // Start Session
            currentSession = new com.library.pos.model.WorkSession(user, LocalDateTime.now());
            sessionRepository.save(currentSession);
        }

        // Show Back Button for Owner
        if (backButton != null) {
            boolean isOwner = user != null && user.getRole() == com.library.pos.model.Role.OWNER;
            backButton.setVisible(isOwner);
            backButton.setManaged(isOwner);
        }

        updateDailyCash();
        updateDataSignature();
    }

    private void updateDailyCash() {
        double salesTotal = 0.0;
        if (currentUser != null) {
            salesTotal = saleService.getDailyCash(currentUser.getId());
        }

        java.math.BigDecimal supplierCashTotal = supplierService.getDailyCashPaymentsTotal();
        double netCash = salesTotal - supplierCashTotal.doubleValue();

        if (dailyCashLabel != null) {
            dailyCashLabel.setText(String.format("%.2f ج.م", netCash));
        }

        if (dailySupplierPaymentsLabel != null) {
            java.math.BigDecimal supplierTotal = supplierService.getDailyPaymentsTotal();
            dailySupplierPaymentsLabel.setText(
                    String.format("مدفوعات الموردين اليوم: %.2f ج.م", supplierTotal.doubleValue()));
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
                    if (event.getCode() == KeyCode.ENTER) {
                        boolean hasCart = !cartItems.isEmpty();
                        boolean barcodeFocused = barcodeField != null && barcodeField.isFocused();
                        boolean barcodeHasText = barcodeField != null
                                && barcodeField.getText() != null
                                && !barcodeField.getText().trim().isEmpty();
                        boolean canPay = hasCart && (!barcodeFocused || !barcodeHasText);
                        if (canPay) {
                            handlePayment();
                            event.consume();
                        }
                    }
                });
            }
        });
    }

    private void handleFunctionKey(KeyCode code) {
        // Quick product shortcuts from configuration
        String barcode = null;
        String keyName = code.getName(); // F1, F2...

        if (quickKeys.containsKey(keyName)) {
            barcode = quickKeys.getProperty(keyName);
        }

        if (barcode != null) {
            searchAndSelectProduct(barcode);
        }
    }

    private void setupCartTableClickHandler() {
        // Allow clicking on cart rows to quickly edit quantity
        cartTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                CartItem selectedItem = cartTable.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    showQuantityEditDialog(selectedItem);
                }
            }
        });
    }

    private void setupSuggestions() {
        if (suggestionList == null || barcodeField == null) {
            return;
        }

        suggestionList.setItems(suggestionItems);
        suggestionList.setVisible(false);
        suggestionList.setManaged(false);
        suggestionList.setFixedCellSize(34);

        suggestionList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String name = item.getName() != null ? item.getName() : "";
                    String barcode = item.getBarcode() != null ? item.getBarcode() : "";
                    setText(name + " | " + barcode);
                    setGraphic(null);
                }
            }
        });

        suggestionList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Product selected = suggestionList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    addProductToCart(selected, 1);
                    hideSuggestions();
                    barcodeField.clear();
                    barcodeField.requestFocus();
                }
            }
        });

        barcodeField.textProperty().addListener((obs, old, value) -> {
            if (value == null || value.trim().isEmpty()) {
                hideSuggestions();
                return;
            }
            List<Product> results = productService.searchByBarcodeOrName(value.trim());
            if (results.isEmpty()) {
                hideSuggestions();
                return;
            }
            showSuggestions(results);
        });

        barcodeField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                hideSuggestions();
            }
        });

        barcodeField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!suggestionList.isVisible()) {
                return;
            }
            if (event.getCode() == KeyCode.DOWN) {
                suggestionList.getSelectionModel().selectNext();
                suggestionList.scrollTo(suggestionList.getSelectionModel().getSelectedIndex());
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                suggestionList.getSelectionModel().selectPrevious();
                suggestionList.scrollTo(suggestionList.getSelectionModel().getSelectedIndex());
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                hideSuggestions();
                event.consume();
            }
        });
    }

    private void setupCustomerSearch() {
        if (customerSearchField == null || customerSearchList == null) {
            return;
        }

        customerSearchList.setItems(customerSearchResults);
        customerSearchList.setVisible(false);
        customerSearchList.setManaged(false);
        customerSearchList.setFixedCellSize(34);

        customerSearchList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String name = item.getCustomerName() != null ? item.getCustomerName() : "";
                    String phone = item.getMobile() != null ? item.getMobile() : "";
                    String code = item.getCustomerCode() != null ? item.getCustomerCode() : "";
                    String display = name;
                    if (!code.isBlank()) {
                        display = display + " | " + code;
                    }
                    if (!phone.isBlank()) {
                        display = display + " | " + phone;
                    }
                    setText(display);
                }
            }
        });

        customerSearchList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Customer selected = customerSearchList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selectCustomer(selected);
                }
            }
        });

        customerSearchField.textProperty().addListener((obs, old, value) -> {
            if (value == null || value.trim().isEmpty()) {
                hideCustomerSuggestions();
                return;
            }
            List<Customer> results = customerService.search(value.trim());
            if (results.isEmpty()) {
                hideCustomerSuggestions();
                return;
            }
            showCustomerSuggestions(results);
        });

        customerSearchField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                hideCustomerSuggestions();
            }
        });

        customerSearchField.setOnAction(e -> {
            String term = customerSearchField.getText();
            if (term == null || term.trim().isEmpty()) {
                hideCustomerSuggestions();
                return;
            }
            processCustomerSearch(term.trim(), true);
        });

        customerSearchField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!customerSearchList.isVisible()) {
                return;
            }
            if (event.getCode() == KeyCode.DOWN) {
                customerSearchList.getSelectionModel().selectNext();
                customerSearchList.scrollTo(customerSearchList.getSelectionModel().getSelectedIndex());
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                customerSearchList.getSelectionModel().selectPrevious();
                customerSearchList.scrollTo(customerSearchList.getSelectionModel().getSelectedIndex());
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                hideCustomerSuggestions();
                event.consume();
            }
        });
    }

    private void processCustomerSearch(String term, boolean allowSuggestions) {
        if (term == null || term.isBlank()) {
            hideCustomerSuggestions();
            return;
        }
        List<Customer> results = customerService.search(term.trim());
        if (results.isEmpty()) {
            hideCustomerSuggestions();
            return;
        }

        String trimmed = term.trim();
        Customer exact = results.stream()
                .filter(c -> matchesCustomerTerm(c, trimmed))
                .findFirst()
                .orElse(null);

        Customer chosen = exact;
        if (chosen == null && allowSuggestions && customerSearchList != null && customerSearchList.isVisible()) {
            Customer selected = customerSearchList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                chosen = selected;
            }
        }
        if (chosen == null) {
            if (!allowSuggestions || results.size() == 1) {
                chosen = results.get(0);
            }
        }

        if (chosen != null) {
            selectCustomer(chosen);
            hideCustomerSuggestions();
        } else if (allowSuggestions) {
            showCustomerSuggestions(results);
        }
    }

    private boolean matchesCustomerTerm(Customer customer, String term) {
        if (customer == null || term == null) {
            return false;
        }
        String code = customer.getCustomerCode();
        String phone = customer.getMobile();
        String name = customer.getCustomerName();
        return (code != null && code.equalsIgnoreCase(term))
                || (phone != null && phone.equalsIgnoreCase(term))
                || (name != null && name.equalsIgnoreCase(term));
    }

    private void showCustomerSuggestions(List<Customer> results) {
        if (customerSearchList == null) {
            return;
        }
        customerSearchResults.setAll(results);
        boolean show = !customerSearchResults.isEmpty();
        customerSearchList.setVisible(show);
        customerSearchList.setManaged(show);
        updateCustomerSuggestionHeight();
        if (show) {
            customerSearchList.getSelectionModel().selectFirst();
        }
    }

    private void hideCustomerSuggestions() {
        if (customerSearchList == null) {
            return;
        }
        customerSearchResults.clear();
        customerSearchList.setVisible(false);
        customerSearchList.setManaged(false);
    }

    private void updateCustomerSuggestionHeight() {
        if (customerSearchList == null) {
            return;
        }
        int count = customerSearchResults.size();
        if (count <= 0) {
            return;
        }
        double height = customerSearchList.getFixedCellSize() * count + 8;
        customerSearchList.setPrefHeight(height);
        customerSearchList.setMinHeight(Region.USE_PREF_SIZE);
        customerSearchList.setMaxHeight(Region.USE_PREF_SIZE);
    }

    private void selectCustomer(Customer customer) {
        selectedCustomer = customer;
        if (customerAddressLabel != null) {
            String address = customer != null ? customer.getAddress() : "";
            customerAddressLabel.setText(address != null && !address.isBlank() ? address : "-");
        }
        if (customerSearchField != null && customer != null) {
            String text = customer.getCustomerCode();
            if (text == null || text.isBlank()) {
                text = customer.getMobile();
            }
            if (text == null || text.isBlank()) {
                text = customer.getCustomerName();
            }
            customerSearchField.setText(text != null ? text : "");
        }
        hideCustomerSuggestions();
        updateOrderButtons();
    }

    private void setupOpenOrdersList() {
        if (openDeliveryOrdersList == null || openStoreOrdersList == null) {
            return;
        }
        openDeliveryOrdersList.setItems(openDeliveryOrders);
        openStoreOrdersList.setItems(openStoreOrders);

        openDeliveryOrdersList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OpenOrder item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    int displayIndex = getIndex() + 1;
                    setText(displayIndex + " - " + formatOpenOrderLabel(item));
                }
            }
        });

        openStoreOrdersList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OpenOrder item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    int displayIndex = getIndex() + 1;
                    setText("طلب " + displayIndex);
                }
            }
        });

        openDeliveryOrdersList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (openStoreOrdersList != null) {
                    openStoreOrdersList.getSelectionModel().clearSelection();
                }
                loadOrderToCart(newVal);
            }
        });

        openStoreOrdersList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (openDeliveryOrdersList != null) {
                    openDeliveryOrdersList.getSelectionModel().clearSelection();
                }
                loadOrderToCart(newVal);
            }
        });
    }

    private void loadDeliveryMen() {
        if (deliveryManCombo == null) {
            return;
        }
        List<User> men = userService.getDeliveryMen();
        deliveryManCombo.setItems(FXCollections.observableArrayList(men));
        deliveryManCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(User user) {
                return formatDeliveryMan(user);
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
        deliveryManCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatDeliveryMan(item));
                }
            }
        });
        deliveryManCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(bundle.getString("cashier.delivery.none"));
                } else {
                    setText(formatDeliveryMan(item));
                }
            }
        });
        deliveryManCombo.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> updateDeliveryManSummary());
    }

    private String formatDeliveryMan(User user) {
        if (user == null) {
            return "";
        }
        String phone = user.getPhoneNumber() != null ? user.getPhoneNumber() : "";
        String name = user.getFullName() != null ? user.getFullName() : "";
        if (!phone.isBlank()) {
            return name + " | " + phone;
        }
        return name;
    }

    private String formatOpenOrderLabel(OpenOrder item) {
        if (item == null) {
            return "";
        }
        Customer customer = item.getCustomer();
        String address = customer != null ? customer.getAddress() : "";
        String name = customer != null ? customer.getCustomerName() : "";
        String label = (address != null && !address.isBlank()) ? address : name;
        if (label == null || label.isBlank()) {
            label = "طلب #" + item.getId();
        }
        return label;
    }

    private void loadOpenOrders() {
        if (openDeliveryOrdersList == null || openStoreOrdersList == null) {
            return;
        }
        List<OpenOrder> all = openOrderService.getOpenOrders();
        openDeliveryOrders.clear();
        openStoreOrders.clear();
        for (OpenOrder order : all) {
            if (isDeliveryOrder(order)) {
                openDeliveryOrders.add(order);
            } else {
                openStoreOrders.add(order);
            }
        }
    }

    @FXML
    private void handleNewOrder() {
        resetOrderState();
    }

    @FXML
    private void handleSaveOrder() {
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.order.alert.empty"));
            return;
        }
        OpenOrder order = activeOrder != null ? activeOrder : new OpenOrder();
        order.setStatus(OpenOrderStatus.OPEN);
        order.setCustomer(selectedCustomer);
        order.setDeliveryMan(deliveryManCombo != null
                ? deliveryManCombo.getSelectionModel().getSelectedItem()
                : null);
        order.clearItems();
        for (CartItem item : cartItems) {
            OpenOrderItem orderItem = new OpenOrderItem(order, item.getProduct(), item.getQuantity(), item.getPrice());
            order.addItem(orderItem);
        }
        OpenOrder saved = openOrderService.save(order);
        activeOrder = saved;
        loadOpenOrders();
        selectOpenOrder(saved);

        showSuccessWindow(bundle.getString("cashier.order.alert.saved"), "رقم الطلب: " + saved.getId());
        resetOrderState();
    }

    private void showSuccessWindow(String message, String details) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/success_popup.fxml"));
            loader.setControllerFactory(applicationContext::getBean);

            javafx.scene.Parent root = loader.load();
            SuccessPopupController controller = loader.getController();
            controller.setMessage(message);
            controller.setDetails(details);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("نجاح");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.INFORMATION, message);
        }
    }

    @FXML
    private void handleDeleteOrder() {
        OpenOrder target = activeOrder != null ? activeOrder : getSelectedOpenOrder();
        if (target == null || target.getId() == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        DialogUtil.initOwner(confirm, workerNameLabel.getScene() != null ? workerNameLabel.getScene().getWindow() : null);
        confirm.setTitle(bundle.getString("cashier.confirm"));
        confirm.setHeaderText(bundle.getString("cashier.order.confirm.delete"));
        confirm.setContentText(bundle.getString("cashier.order.confirm.delete.message"));
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        openOrderService.delete(target);
        if (activeOrder != null && target.getId().equals(activeOrder.getId())) {
            activeOrder = null;
            cartItems.clear();
            updateSummary();
        }
        loadOpenOrders();
        resetOrderState();
        showAlert(Alert.AlertType.INFORMATION, bundle.getString("cashier.order.alert.deleted"));
    }

    private void selectOpenOrder(OpenOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        for (OpenOrder item : openDeliveryOrders) {
            if (item.getId().equals(order.getId())) {
                if (openDeliveryOrdersList != null) {
                    openDeliveryOrdersList.getSelectionModel().select(item);
                }
                break;
            }
        }
        for (OpenOrder item : openStoreOrders) {
            if (item.getId().equals(order.getId())) {
                if (openStoreOrdersList != null) {
                    openStoreOrdersList.getSelectionModel().select(item);
                }
                break;
            }
        }
    }

    private boolean isDeliveryOrder(OpenOrder order) {
        if (order == null) {
            return false;
        }
        return order.getDeliveryMan() != null;
    }

    private OpenOrder getSelectedOpenOrder() {
        if (openDeliveryOrdersList != null) {
            OpenOrder selected = openDeliveryOrdersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                return selected;
            }
        }
        if (openStoreOrdersList != null) {
            return openStoreOrdersList.getSelectionModel().getSelectedItem();
        }
        return null;
    }

    private void loadOrderToCart(OpenOrder order) {
        activeOrder = order;
        Customer customer = order.getCustomer();
        if (customer != null) {
            selectCustomer(customer);
        } else {
            selectedCustomer = null;
            if (customerSearchField != null) {
                customerSearchField.clear();
            }
            if (customerAddressLabel != null) {
                customerAddressLabel.setText("-");
            }
        }
        if (deliveryManCombo != null) {
            User deliveryMan = order.getDeliveryMan();
            if (deliveryMan != null) {
                deliveryManCombo.getSelectionModel().select(deliveryMan);
            } else {
                deliveryManCombo.getSelectionModel().clearSelection();
                deliveryManCombo.setValue(null);
            }
        }
        cartItems.clear();
        for (OpenOrderItem item : order.getItems()) {
            cartItems.add(new CartItem(item.getProduct(), item.getQuantity()));
        }
        updateSummary();
        updateOrderButtons();
        updateDeliveryManSummary();
    }

    private void updateOrderButtons() {
        boolean hasCart = !cartItems.isEmpty();
        if (newOrderButton != null) {
            newOrderButton.setDisable(false);
        }
        if (saveOrderButton != null) {
            saveOrderButton.setDisable(!hasCart);
        }
        if (deleteOrderButton != null) {
            deleteOrderButton.setDisable(activeOrder == null || activeOrder.getId() == null);
        }
    }

    private void updateDeliveryManSummary() {
        if (deliveryManSummaryLabel == null) {
            return;
        }
        User deliveryMan = deliveryManCombo != null
                ? deliveryManCombo.getSelectionModel().getSelectedItem()
                : null;
        boolean show = deliveryMan != null;
        deliveryManSummaryLabel.setVisible(show);
        deliveryManSummaryLabel.setManaged(show);
        if (show) {
            deliveryManSummaryLabel.setText("التوصيل: " + formatDeliveryMan(deliveryMan));
        } else {
            deliveryManSummaryLabel.setText("");
        }
    }

    private void showQuantityEditDialog(CartItem item) {
        // DialogUtil creates the stage

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12;");

        // Current quantity info
        HBox infoBox = new HBox(10);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setStyle("-fx-background-color: #334155; -fx-padding: 12; -fx-background-radius: 8;");
        Label infoLabel = new Label("الكمية الحالية: " + item.getQuantity());
        infoLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8;");
        infoBox.getChildren().add(infoLabel);

        // Quantity input
        VBox qtyBox = new VBox(8);
        Label qtyLabel = new Label("الكمية الجديدة:");
        qtyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
        TextField qtyField = new TextField(String.valueOf(item.getQuantity()));
        qtyField.setStyle("-fx-font-size: 28px; -fx-background-color: #334155; -fx-text-fill: white; " +
                "-fx-border-color: #475569; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-padding: 12; -fx-alignment: center;");
        qtyField.selectAll();
        qtyBox.getChildren().addAll(qtyLabel, qtyField);

        Stage dialog = DialogUtil.createDialog("تعديل الكمية - " + item.getProduct().getName(), root,
                workerNameLabel.getScene().getWindow());

        // Buttons
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button("إلغاء");
        cancelBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 12 24; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button("تأكيد");
        confirmBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 12 32; -fx-background-radius: 8; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> {
            try {
                int newQty = Integer.parseInt(qtyField.getText().trim());
                if (newQty <= 0) {
                    showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.qty"));
                    return;
                }
                if (newQty > item.getProduct().getQuantity()) {
                    showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.stock"));
                    return;
                }
                item.setQuantity(newQty);
                cartTable.refresh();
                updateSummary();
                dialog.close();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.qty"));
            }
        });
        buttons.getChildren().addAll(cancelBtn, confirmBtn);

        // Enter key to confirm
        qtyField.setOnAction(e -> confirmBtn.fire());

        // Remove explicit header, use title
        root.getChildren().addAll(infoBox, qtyBox, buttons);

        // Ensure size is good
        root.setPrefWidth(400);

        javafx.application.Platform.runLater(() -> qtyField.requestFocus());
        dialog.showAndWait();
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
        if (term == null || term.isBlank()) {
            hideSuggestions();
            return;
        }
        processSearch(term.trim(), true);
    }

    @FXML
    private void handleSearch() {
        handleBarcodeEnter();
    }

    private void searchAndSelectProduct(String term) {
        processSearch(term, false);
    }

    private void processSearch(String term, boolean allowSuggestions) {
        if (term == null || term.isBlank()) {
            hideSuggestions();
            return;
        }

        String trimmed = term.trim();
        List<Product> results = productService.searchByBarcodeOrName(trimmed);
        if (results.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.notfound"));
            resetSelection();
            if (barcodeField != null) {
                barcodeField.selectAll();
            }
            hideSuggestions();
            return;
        }

        Product exact = results.stream()
                .filter(p -> p.getBarcode() != null && p.getBarcode().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);

        Product chosen = exact;

        if (chosen == null && allowSuggestions && suggestionList != null && suggestionList.isVisible()) {
            Product selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                chosen = selected;
            }
        }

        if (chosen == null) {
            if (!allowSuggestions || results.size() == 1) {
                chosen = results.get(0);
            }
        }

        if (chosen != null) {
            addProductToCart(chosen, 1);
            if (barcodeField != null) {
                barcodeField.clear();
                barcodeField.requestFocus();
            }
            hideSuggestions();
        } else if (allowSuggestions) {
            showSuggestions(results);
        }
    }

    private void showSuggestions(List<Product> results) {
        if (suggestionList == null) {
            return;
        }
        suggestionItems.setAll(results);
        boolean show = !suggestionItems.isEmpty();
        suggestionList.setVisible(show);
        suggestionList.setManaged(show);
        updateSuggestionHeight();
        if (show) {
            suggestionList.getSelectionModel().selectFirst();
        }
    }

    private void hideSuggestions() {
        if (suggestionList == null) {
            return;
        }
        suggestionItems.clear();
        suggestionList.setVisible(false);
        suggestionList.setManaged(false);
    }

    private void updateSuggestionHeight() {
        if (suggestionList == null) {
            return;
        }
        int count = suggestionItems.size();
        if (count <= 0) {
            return;
        }
        double height = suggestionList.getFixedCellSize() * count + 8;
        suggestionList.setPrefHeight(height);
        suggestionList.setMinHeight(Region.USE_PREF_SIZE);
        suggestionList.setMaxHeight(Region.USE_PREF_SIZE);
    }

    private void addProductToCart(Product product, int qty) {
        // Check stock
        if (qty > product.getQuantity()) {
            showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.stock"));
            return;
        }

        // Add to cart or update existing
        Optional<CartItem> existing = cartItems.stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existing.isPresent()) {
            int newQty = existing.get().getQuantity() + qty;
            if (newQty > product.getQuantity()) {
                showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.stock"));
                return;
            }
            existing.get().setQuantity(newQty);
            cartTable.refresh();
        } else {
            cartItems.add(new CartItem(product, qty));
        }

        updateSummary();
    }

    private void selectProduct(Product p) {
        this.selectedProduct = p;
        if (productNameLabel != null) {
            productNameLabel.setText(p.getName());
        }
        if (priceLabel != null) {
            priceLabel.setText(String.format("%.2f", p.getSellPrice()));
        }
        if (stockLabel != null) {
            stockLabel.setText(String.valueOf(p.getQuantity()));
        }
        if (barcodeDisplayLabel != null) {
            barcodeDisplayLabel.setText(p.getBarcode());
        }

        // Update stock label color based on quantity
        if (stockLabel != null) {
            if (p.getQuantity() <= 0) {
                stockLabel.setStyle("-fx-text-fill: #ef4444;");
            } else if (p.getQuantity() <= p.getMinStock()) {
                stockLabel.setStyle("-fx-text-fill: #f59e0b;");
            } else {
                stockLabel.setStyle("-fx-text-fill: #94a3b8;");
            }
        }

        if (addButton != null) {
            addButton.setDisable(false);
        }
        if (qtyField != null) {
            qtyField.setText("1");
            qtyField.requestFocus();
            qtyField.selectAll();
        }
    }

    private void resetSelection() {
        selectedProduct = null;
        if (productNameLabel != null) {
            productNameLabel.setText("-");
        }
        if (priceLabel != null) {
            priceLabel.setText("0.00");
        }
        if (stockLabel != null) {
            stockLabel.setText("-");
            stockLabel.setStyle("-fx-text-fill: #94a3b8;");
        }
        if (barcodeDisplayLabel != null) {
            barcodeDisplayLabel.setText("-");
        }
        if (addButton != null) {
            addButton.setDisable(true);
        }
        if (qtyField != null) {
            qtyField.setText("1");
        }
    }

    @FXML
    private void handleQtyMinus() {
        if (qtyField == null) {
            return;
        }
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
        if (qtyField == null) {
            return;
        }
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
            int qty = 1;
            if (qtyField != null) {
                qty = Integer.parseInt(qtyField.getText().trim());
            }
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
        DialogUtil.initOwner(confirm, workerNameLabel.getScene() != null ? workerNameLabel.getScene().getWindow() : null);
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
        updateOrderButtons();
    }

    @FXML
    private void handlePayment() {
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.empty"));
            return;
        }
        String selected = paymentMethodCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "الرجاء اختيار طريقة الدفع");
            return;
        }
        // Extract payment method (remove emoji and parentheses)
        // If no parentheses, keep full label to avoid truncating values like "Vodafone
        // Cash"
        String paymentMethod = selected.contains("(")
                ? selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"))
                : selected;
        showPaymentDialog(paymentMethod);
    }

    @FXML
    private void handleCashPayment() {
        // Keep for backward compatibility if needed
        handlePayment();
    }

    @FXML
    private void handleDeferredPayment() {
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.empty"));
            return;
        }
        showDeferredPaymentDialog();
    }

    private void showPaymentDialog(String paymentMethod) {
        double total = cartItems.stream().mapToDouble(CartItem::getTotal).sum();

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12;");

        // Header content (Method label only, Title in Dialog)
        Label methodLabel = new Label("طريقة الدفع: " + paymentMethod);
        methodLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #10b981;");

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

        // Update change on input (Listeners)
        cashField.textProperty().addListener((obs, old, newVal) -> {
            try {
                double received = parseAmount(newVal);
                double change = received - total;
                changeValue.setText(String.format("%.2f ج.م", Math.max(0, change)));
                changeValue.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; " +
                        "-fx-text-fill: " + (change >= 0 ? "#10b981" : "#ef4444") + ";");
            } catch (NumberFormatException e) {
                changeValue.setText("0.00 ج.م");
            }
        });
        // Default cash received to total amount
        cashField.setText(String.format("%.2f", total));
        cashField.selectAll();

        // Dialog creation
        Stage dialog = DialogUtil.createDialog("تأكيد الدفع", root, workerNameLabel.getScene().getWindow());

        // Buttons
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button(bundle.getString("cashier.cancel"));
        cancelBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 12 24; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button(bundle.getString("cashier.confirm"));
        confirmBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 12 32; -fx-background-radius: 8; -fx-cursor: hand;");
        confirmBtn.setDefaultButton(true);
        confirmBtn.setOnAction(e -> {
            try {
                double received = parseAmount(cashField.getText());
                if (received < total) {
                    showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.insufficient"));
                    return;
                }
                completeSale(paymentMethod, null);
                dialog.close();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, bundle.getString("cashier.error.amount"));
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "خطأ في عملية الدفع: " + ex.getMessage());
            }
        });
        buttons.getChildren().addAll(cancelBtn, confirmBtn);

        root.getChildren().addAll(methodLabel, totalBox, cashBox, changeBox, buttons);
        root.setPrefWidth(450);

        dialog.setOnShown(e -> cashField.requestFocus());
        dialog.showAndWait();
    }

    private void showDeferredPaymentDialog() {
        double total = cartItems.stream().mapToDouble(CartItem::getTotal).sum();

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12;");

        // Header Title in DialogUtil

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

        Stage dialog = DialogUtil.createDialog(bundle.getString("cashier.deferred"), root,
                workerNameLabel.getScene().getWindow());

        // Buttons
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button(bundle.getString("cashier.cancel"));
        cancelBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 12 24; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button(bundle.getString("cashier.register.deferred"));
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

        root.getChildren().addAll(totalBox, customerBox, phoneBox, buttons);
        root.setPrefWidth(450);

        dialog.showAndWait();
    }

    private void completeSale(String paymentType, String customerName) {
        // Fallback user if null (prevent crash)
        User worker = currentUser;
        if (worker == null) {
            System.out.println("WARNING: currentUser is null. Attempting to fallback to first worker found.");
            worker = userService.getAllWorkers().stream().findFirst().orElse(null);
            if (worker == null) {
                // Even worse, try ANY user
                // But userService.getAllWorkers returns only WORKERS.
                // We might need a generic findFirst.
                // However, assuming there is at least one worker or admin.
                // If worker still null, we risk crash, but let's try creating a dummy or error.
                // Since we can't create dummy with ID easily (JPA), we throw meaningful error.
                throw new RuntimeException("No active user and no fallback worker found in DB!");
            }
            // Temporarily set currentUser so session tracking works? No, just use 'worker'
            // for sale.
        }

        Customer orderCustomer = selectedCustomer;
        User assignedDeliveryMan = deliveryManCombo != null
                ? deliveryManCombo.getSelectionModel().getSelectedItem()
                : null;

        // Process sales
        for (CartItem item : cartItems) {
            String baseNotes = paymentType.equals("DEFERRED")
                    ? "Deferred: " + customerName
                    : paymentType + " Payment";
            String notes = baseNotes;
            if (orderCustomer != null) {
                StringBuilder extra = new StringBuilder();
                extra.append("Customer: ").append(orderCustomer.getCustomerName());
                String address = orderCustomer.getAddress();
                if (address != null && !address.isBlank()) {
                    extra.append(", Address: ").append(address);
                }
                if (assignedDeliveryMan != null) {
                    extra.append(", Delivery: ").append(assignedDeliveryMan.getFullName());
                }
                notes = baseNotes + " | " + extra;
            }
            Sale sale = new Sale(
                    java.time.LocalDateTime.now(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getTotal(),
                    com.library.pos.model.SaleStatus.SOLD,
                    worker,
                    notes);

            if (orderCustomer != null) {
                sale.setCustomer(orderCustomer);
            }

            sale.setProduct(item.getProduct());
            if (paymentType.equals("DEFERRED")) {
                sale.setStatus(com.library.pos.model.SaleStatus.DEFERRED);
            }

            saleService.save(sale);
        }
        cartItems.clear();
        updateSummary();
        resetSelection();
        barcodeField.requestFocus();

        showAlert(Alert.AlertType.INFORMATION, bundle.getString("cashier.success"));
        clearActiveOrderAfterPayment();
        resetOrderState();
    }

    private void resetOrderState() {
        activeOrder = null;
        selectedCustomer = null;
        cartItems.clear();
        updateSummary();
        if (customerSearchField != null) {
            customerSearchField.clear();
        }
        if (customerAddressLabel != null) {
            customerAddressLabel.setText("-");
        }
        hideCustomerSuggestions();
        if (deliveryManCombo != null) {
            deliveryManCombo.getSelectionModel().clearSelection();
            deliveryManCombo.setValue(null);
        }
        if (openDeliveryOrdersList != null) {
            openDeliveryOrdersList.getSelectionModel().clearSelection();
        }
        if (openStoreOrdersList != null) {
            openStoreOrdersList.getSelectionModel().clearSelection();
        }
        updateDeliveryManSummary();
        updateOrderButtons();
    }

    private void clearActiveOrderAfterPayment() {
        OpenOrder target = activeOrder != null ? activeOrder : getSelectedOpenOrder();
        if (target != null && target.getId() != null) {
            openOrderService.delete(target);
            loadOpenOrders();
        }
        activeOrder = null;
        updateOrderButtons();
    }

    private double parseAmount(String value) {
        if (value == null) {
            throw new NumberFormatException("null");
        }
        String normalized = value.trim()
                .replace('٠', '0')
                .replace('١', '1')
                .replace('٢', '2')
                .replace('٣', '3')
                .replace('٤', '4')
                .replace('٥', '5')
                .replace('٦', '6')
                .replace('٧', '7')
                .replace('٨', '8')
                .replace('٩', '9')
                .replace('٫', '.')
                .replace('٬', ',')
                .replace(",", "");
        return Double.parseDouble(normalized);
    }

    @FXML
    private void handleQuickBtn(ActionEvent event) {
        if (event.getSource() instanceof Button) {
            Button btn = (Button) event.getSource();
            String text = btn.getText(); // "F1", "F2", etc.

            if (quickKeys.containsKey(text)) {
                String barcode = quickKeys.getProperty(text);
                searchAndSelectProduct(barcode);
                // Return focus to barcode field
                if (barcodeField != null) {
                    barcodeField.requestFocus();
                }
            }
        }
    }

    @FXML
    public void handleBackToDashboard() {
        try {
            Stage stage = (Stage) workerNameLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            loader.setResources(bundle);
            javafx.scene.Parent root = loader.load();

            DashboardController controller = loader.getController();
            controller.setUser(currentUser);

            // Seamless transition
            Scene currentScene = workerNameLabel.getScene();
            currentScene.setRoot(root);

            stage.setTitle(bundle.getString("app.title"));
            if (!stage.isFullScreen()) {
                stage.setFullScreen(true);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
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

            // Seamless transition
            Scene currentScene = barcodeField.getScene();
            currentScene.setRoot(loader.load());

            stage.setTitle(bundle.getString("app.title"));
            if (!stage.isFullScreen()) {
                stage.setFullScreen(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        DialogUtil.initOwner(alert, barcodeField.getScene() != null ? barcodeField.getScene().getWindow() : null);
        alert.setContentText(msg);
        alert.show();
    }

    @FXML
    private void handleSupplierPayment() {
        showSupplierPaymentDialog();
    }

    @FXML
    private void handleReturns() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/returns_popup.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Scene scene = new Scene(loader.load());

            // Add CSS styling
            java.net.URL css = getClass().getResource("/css/style.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }

            ReturnsPopupController controller = loader.getController();
            controller.setCurrentWorker(currentUser);
            controller.setOnReturnProcessed(() -> {
                // Refresh data after return is processed
                updateDailyCash();
                updateDataSignature();
            });

            Stage dialog = new Stage();

            // Fix: Set owner to keep full screen
            if (workerNameLabel != null && workerNameLabel.getScene() != null) {
                dialog.initOwner(workerNameLabel.getScene().getWindow());
            }

            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.setTitle("معالجة المرتجعات");
            dialog.setMinWidth(500);
            dialog.setMinHeight(500);
            dialog.setScene(scene);

            // Center on screen
            dialog.centerOnScreen();

            dialog.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "خطأ في فتح شاشة المرتجعات: " + ex.getMessage());
        }
    }

    @FXML
    private void handleAddCustomer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/delivery_popup.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Scene scene = new Scene(loader.load());

            DeliveryPopupController controller = loader.getController();
            controller.selectedCustomerProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectCustomer(newVal);
                }
            });

            Stage dialog = new Stage();

            // Fix: Set owner
            if (BarcodeField() != null && BarcodeField().getScene() != null) {
                dialog.initOwner(BarcodeField().getScene().getWindow());
            } else if (workerNameLabel != null && workerNameLabel.getScene() != null) {
                dialog.initOwner(workerNameLabel.getScene().getWindow());
            }

            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.setTitle("عميل جديد");
            dialog.setScene(scene);
            dialog.centerOnScreen();
            dialog.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "خطأ في فتح شاشة إضافة العميل: " + ex.getMessage());
        }
    }

    // Helper to get barcode field safely or use other node
    private javafx.scene.Node BarcodeField() {
        return barcodeField;
    }

    private void showSupplierPaymentDialog() {
        // Build payment form
        VBox formCard = new VBox(12);
        formCard.setStyle(
                "-fx-background-color: #0f172a; -fx-padding: 30; -fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");
        formCard.setMaxWidth(450);
        formCard.setMaxHeight(520);

        ComboBox<Supplier> supplierCombo = new ComboBox<>();
        List<Supplier> allSuppliers = supplierService.listAll(true);
        List<Supplier> uniqueSuppliers = allSuppliers.stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(
                                () -> new java.util.TreeSet<>(java.util.Comparator.comparing(Supplier::toString))),
                        java.util.ArrayList::new));
        supplierCombo.setItems(FXCollections.observableArrayList(uniqueSuppliers));
        supplierCombo.setPromptText("اختر المورد");
        supplierCombo.setMaxWidth(Double.MAX_VALUE);

        TextField amountField = new TextField();
        amountField.setPromptText("المبلغ المدفوع اليوم");

        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.setItems(FXCollections.observableArrayList("Cash", "InstaPay", "Visa", "Vodafone Cash"));
        methodCombo.getSelectionModel().selectFirst();
        methodCombo.setMaxWidth(Double.MAX_VALUE);

        TextArea notesField = new TextArea();
        notesField.setPromptText("ملاحظات");
        notesField.setPrefRowCount(2);

        // Build the overlay (dark background + card)
        VBox overlay = new VBox();
        overlay.setAlignment(javafx.geometry.Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

        // Title row
        HBox titleRow = new HBox();
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label titleLabel = new Label("دفعة للمورد");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 18px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> cashierRootStack.getChildren().remove(overlay));
        titleRow.getChildren().addAll(titleLabel, closeBtn);

        // Buttons
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Button cancelBtn = new Button("إلغاء");
        cancelBtn.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-padding: 8 20; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> cashierRootStack.getChildren().remove(overlay));
        Button saveBtn = new Button("تسجيل");
        saveBtn.setStyle(
                "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(e -> {
            try {
                Supplier supplier = supplierCombo.getSelectionModel().getSelectedItem();
                if (supplier == null) {
                    showInAppMessage("يرجى اختيار المورد", "#ef4444");
                    return;
                }
                String amtStr = amountField.getText();
                if (amtStr == null || amtStr.isBlank()) {
                    showInAppMessage("يرجى إدخال المبلغ", "#ef4444");
                    return;
                }
                java.math.BigDecimal amount = java.math.BigDecimal.valueOf(parseAmount(amtStr));
                SupplierPayment payment = new SupplierPayment();
                String selectedMethod = methodCombo.getSelectionModel().getSelectedItem();
                payment.setSupplier(supplier);
                payment.setAmount(amount);
                payment.setMethod(mapSupplierPaymentMethod(selectedMethod));
                payment.setMethodDisplay(selectedMethod);
                payment.setNotes(notesField.getText());
                supplierService.recordPayment(payment);
                updateDailyCash();
                updateDataSignature();
                cashierRootStack.getChildren().remove(overlay);
                showInAppMessage("تم تسجيل دفعة المورد بنجاح", "#10b981");
            } catch (Exception ex) {
                ex.printStackTrace();
                showInAppMessage("خطأ: " + ex.getMessage(), "#ef4444");
            }
        });
        btnBox.getChildren().addAll(cancelBtn, saveBtn);

        // Labels
        Label l1 = new Label("المورد");
        l1.setStyle("-fx-text-fill: #cbd5e1;");
        Label l2 = new Label("المبلغ");
        l2.setStyle("-fx-text-fill: #cbd5e1;");
        Label l3 = new Label("طريقة الدفع");
        l3.setStyle("-fx-text-fill: #cbd5e1;");
        Label l4 = new Label("ملاحظات");
        l4.setStyle("-fx-text-fill: #cbd5e1;");

        formCard.getChildren().addAll(titleRow, l1, supplierCombo, l2, amountField, l3, methodCombo, l4, notesField,
                btnBox);
        overlay.getChildren().add(formCard);

        // Add overlay to the root StackPane
        cashierRootStack.getChildren().add(overlay);
    }

    /**
     * Show a brief in-app message overlay (success or error).
     */
    private void showInAppMessage(String message, String borderColor) {
        VBox overlay = new VBox();
        overlay.setAlignment(javafx.geometry.Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

        String icon = borderColor.contains("10b981") ? "✓" : "⚠";
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 36px; -fx-text-fill: " + borderColor + ";");

        Label msgLabel = new Label(message);
        msgLabel.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-wrap-text: true; -fx-text-alignment: center;");
        msgLabel.setMaxWidth(350);

        Button okBtn = new Button("موافق");
        okBtn.setStyle("-fx-background-color: " + borderColor
                + "; -fx-text-fill: white; -fx-padding: 10 30; -fx-cursor: hand; -fx-font-weight: bold;");
        okBtn.setOnAction(e -> cashierRootStack.getChildren().remove(overlay));

        VBox card = new VBox(16, iconLabel, msgLabel, okBtn);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setStyle("-fx-background-color: #0f172a; -fx-padding: 40; -fx-border-color: " + borderColor
                + "; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");
        card.setMaxWidth(400);
        card.setMaxHeight(200);

        overlay.getChildren().add(card);
        cashierRootStack.getChildren().add(overlay);
    }

    private PaymentMethod mapSupplierPaymentMethod(String method) {
        if (method == null) {
            return PaymentMethod.CASH;
        }
        return switch (method) {
            case "Cash" -> PaymentMethod.CASH;
            case "InstaPay", "Visa", "Vodafone Cash" -> PaymentMethod.BANK;
            default -> PaymentMethod.OTHER;
        };
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

    private class PaymentMethodListCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item);

                String imagePath = null;
                if (item.contains("InstaPay")) {
                    imagePath = "/images/instapay.png";
                } else if (item.contains("Vodafone")) {
                    imagePath = "/images/vodafone_cash.png";
                }

                if (imagePath != null) {
                    try {
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                                new javafx.scene.image.Image(getClass().getResourceAsStream(imagePath)));
                        iv.setFitHeight(24);
                        iv.setFitWidth(24);
                        iv.setPreserveRatio(true);
                        setGraphic(iv);
                    } catch (Exception e) {
                        System.err.println("Failed to load icon: " + imagePath);
                        setGraphic(null);
                    }
                } else {
                    setGraphic(null);
                }
            }
        }
    }
}
