package com.library.pos.controller;

import com.library.pos.model.Customer;
import com.library.pos.service.CustomerService;
import com.library.pos.util.AutoRefreshUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomersController {

    @FXML
    private TableView<Customer> customersTable;
    @FXML
    private TableColumn<Customer, Long> idCol;
    @FXML
    private TableColumn<Customer, String> nameCol;
    @FXML
    private TableColumn<Customer, String> phoneCol;

    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField searchField;
    @FXML
    private Button mainActionBtn;

    private final CustomerService customerService;
    private Long editingId = null;
    private Timeline autoRefreshTimeline;
    private static final int AUTO_REFRESH_SECONDS = 3;

    public CustomersController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("mobile"));

        setupActions();
        loadData();
        setupAutoRefresh();
    }

    @FXML
    public void loadData() {
        customersTable.setItems(FXCollections.observableArrayList(customerService.getAll()));
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
    }

    private void setupAutoRefresh() {
        if (customersTable == null) {
            return;
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(AUTO_REFRESH_SECONDS), e -> refreshFromCurrentFilter()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
        AutoRefreshUtil.bind(autoRefreshTimeline, customersTable, 0.5);
    }

    @FXML
    public void handleSearch() {
        String term = searchField.getText();
        if (term == null || term.isBlank()) {
            loadData();
            return;
        }
        customersTable.setItems(FXCollections.observableArrayList(customerService.search(term.trim())));
    }

    @FXML
    public void handleSave() {
        String name = nameField.getText();
        String phone = phoneField.getText();

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

        customerService.save(customer);
        clearForm();
        loadData();
    }

    private void clearForm() {
        nameField.clear();
        phoneField.clear();
        editingId = null;
        mainActionBtn.setText("حفظ البيانات");
    }

    private void setupActions() {
        TableColumn<Customer, Void> colBtn = new TableColumn<>("إجراءات");
        colBtn.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnDelete = new Button("❌");
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
                    mainActionBtn.setText("تحديث العميل");
                });

                btnDelete.setOnAction(e -> {
                    Customer c = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "حذف العميل " + c.getCustomerName() + "؟",
                            ButtonType.YES, ButtonType.NO);
                    if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        customerService.deleteById(c.getId());
                        loadData();
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
        new Alert(Alert.AlertType.INFORMATION, msg).show();
    }
}
