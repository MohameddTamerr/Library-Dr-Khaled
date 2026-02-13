package com.library.pos.controller;

import com.library.pos.model.Customer;
import com.library.pos.service.CustomerService;
import com.library.pos.util.AutoRefreshUtil;
import com.library.pos.util.DialogUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Component
public class CustomersController {

    @FXML
    private TableView<Customer> customersTable;
    @FXML
    private TableColumn<Customer, Long> idCol;
    @FXML
    private TableColumn<Customer, String> codeCol;
    @FXML
    private TableColumn<Customer, String> nameCol;
    @FXML
    private TableColumn<Customer, String> phoneCol;

    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField searchField;
    @FXML
    private Button mainActionBtn;

    private final CustomerService customerService;
    private Long editingId = null;
    private Timeline autoRefreshTimeline;
    private static final int REFRESH_SECONDS_VISIBLE = 8;
    private static final int REFRESH_SECONDS_HIDDEN = 16;
    private LocalDateTime lastUpdatedAt;
    private long lastCount = -1;

    public CustomersController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        codeCol.setCellValueFactory(new PropertyValueFactory<>("customerCode"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("mobile"));

        setupActions();
        loadData();
        updateDataSignature();
        setupAutoRefresh();
    }

    @FXML
    public void loadData() {
        customersTable.setItems(FXCollections.observableArrayList(customerService.getAll()));
        updateDataSignature();
    }

    private void refreshFromCurrentFilter() {
        if (customersTable == null) {
            return;
        }
        String term = searchField != null ? searchField.getText() : null;
        if (term != null && !term.isBlank()) {
            customersTable.setItems(FXCollections.observableArrayList(customerService.search(term.trim())));
        } else {
            loadData();
        }
        updateDataSignature();
    }

    private void setupAutoRefresh() {
        if (customersTable == null) {
            return;
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(REFRESH_SECONDS_VISIBLE), e -> {
            if (shouldRefresh()) {
                refreshFromCurrentFilter();
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
        AutoRefreshUtil.bind(autoRefreshTimeline, customersTable,
                (double) REFRESH_SECONDS_VISIBLE / (double) REFRESH_SECONDS_HIDDEN);
    }

    private boolean shouldRefresh() {
        if (isUserEditing()) {
            return false;
        }
        return hasDataChanged();
    }

    private boolean isUserEditing() {
        return (nameField != null && nameField.isFocused())
                || (phoneField != null && phoneField.isFocused())
                || (searchField != null && searchField.isFocused());
    }

    private boolean hasDataChanged() {
        LocalDateTime latest = customerService.getLatestUpdateTime();
        long count = customerService.getTotalCount();
        boolean changed = !Objects.equals(latest, lastUpdatedAt) || count != lastCount;
        if (changed) {
            lastUpdatedAt = latest;
            lastCount = count;
        }
        return changed;
    }

    private void updateDataSignature() {
        lastUpdatedAt = customerService.getLatestUpdateTime();
        lastCount = customerService.getTotalCount();
    }

    @FXML
    public void handleSearch() {
        String term = searchField.getText();
        if (term == null || term.isBlank()) {
            loadData();
            return;
        }
        customersTable.setItems(FXCollections.observableArrayList(customerService.search(term.trim())));
        updateDataSignature();
    }

    @FXML
    public void handleSave() {
        String name = nameField.getText();
        String phone = phoneField.getText();
        String address = addressField != null ? addressField.getText() : null;

        if (name == null || name.isBlank()) {
            showAlert("يرجى إدخال الاسم");
            return;
        }

        Customer customer;
        if (editingId != null) {
            Optional<Customer> existing = customerService.getAll().stream().filter(c -> c.getId().equals(editingId))
                    .findFirst();
            customer = existing.orElse(new Customer());
        } else {
            customer = new Customer();
        }

        customer.setCustomerName(name);
        customer.setMobile(phone);
        customer.setAddress(address != null ? address.trim() : "");

        customerService.save(customer);
        clearForm();
        loadData();
    }

    private void clearForm() {
        nameField.clear();
        phoneField.clear();
        if (addressField != null) {
            addressField.clear();
        }
        editingId = null;
        mainActionBtn.setText("حفظ البيانات");
    }

    private void setupActions() {
        TableColumn<Customer, Void> colBtn = new TableColumn<>("إجراءات");
        colBtn.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("تعديل");
            private final Button btnDelete = new Button("حذف");
            private final HBox pane = new HBox(10, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("button-warning-large"); // Reusing valid style or simple style
                btnEdit.setStyle(
                        "-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-color: #f59e0b; -fx-text-fill: white;");

                btnDelete.setStyle(
                        "-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-color: #ef4444; -fx-text-fill: white;");

                btnEdit.setOnAction(e -> {
                    Customer c = getTableView().getItems().get(getIndex());
                    editingId = c.getId();
                    nameField.setText(c.getCustomerName());
                    phoneField.setText(c.getMobile());
                    if (addressField != null) {
                        addressField.setText(c.getAddress());
                    }
                    mainActionBtn.setText("تحديث العميل");
                });

                btnDelete.setOnAction(e -> {
                    Customer c = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "حذف العميل " + c.getCustomerName() + "؟",
                            ButtonType.YES, ButtonType.NO);
                    DialogUtil.initOwner(alert, customersTable.getScene() != null ? customersTable.getScene().getWindow() : null);
                    if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        try {
                            customerService.deleteById(c.getId());
                            loadData();
                        } catch (Exception ex) {
                            showAlert("لا يمكن حذف العميل لأنه مرتبط بطلبات/مبيعات سابقة.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        customersTable.getColumns().add(colBtn);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        DialogUtil.initOwner(alert, customersTable.getScene() != null ? customersTable.getScene().getWindow() : null);
        alert.show();
    }
}
