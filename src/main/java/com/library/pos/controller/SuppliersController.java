package com.library.pos.controller;

import com.library.pos.model.PaymentMethod;
import com.library.pos.model.Purchase;
import com.library.pos.model.Supplier;
import com.library.pos.model.SupplierPayment;
import com.library.pos.model.SupplierStatus;
import com.library.pos.service.SupplierService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import javafx.scene.layout.VBox;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class SuppliersController {

    @FXML
    private TableView<Supplier> suppliersTable;
    @FXML
    private TableColumn<Supplier, String> nameCol;
    @FXML
    private TableColumn<Supplier, String> phoneCol;
    @FXML
    private TableColumn<Supplier, BigDecimal> totalPurchasesCol;
    @FXML
    private TableColumn<Supplier, BigDecimal> totalPaidCol;
    @FXML
    private TableColumn<Supplier, BigDecimal> balanceDueCol;
    @FXML
    private TableColumn<Supplier, String> statusCol;
    @FXML
    private TableColumn<Supplier, LocalDateTime> createdAtCol;
    @FXML
    private TableView<SupplierPayment> paymentsTable;
    @FXML
    private TableColumn<SupplierPayment, BigDecimal> paymentAmountCol;
    @FXML
    private TableColumn<SupplierPayment, String> paymentMethodCol;
    @FXML
    private TableColumn<SupplierPayment, LocalDateTime> paymentDateCol;
    @FXML
    private TableColumn<SupplierPayment, String> paymentNotesCol;

    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField totalPurchasesField;
    @FXML
    private CheckBox statusCheckBox;
    @FXML
    private TextField searchField;
    @FXML
    private Label totalPurchasesLabel;
    @FXML
    private Label totalPaidLabel;
    @FXML
    private Label balanceDueLabel;
    @FXML
    private VBox successOverlay;
    @FXML
    private Label successMessageLabel;
    @FXML
    private Label successDetailsLabel;
    @FXML
    private VBox paymentOverlay;
    @FXML
    private ComboBox<Supplier> paymentSupplierCombo;
    @FXML
    private TextField paymentAmountField;
    @FXML
    private ComboBox<String> paymentMethodCombo;
    @FXML
    private TextArea paymentNotesField;
    @FXML
    private VBox errorOverlay;
    @FXML
    private Label errorMessageLabel;

    private final SupplierService supplierService;
    private final org.springframework.context.ApplicationContext applicationContext;
    private Long editingId;

    public SuppliersController(SupplierService supplierService,
            org.springframework.context.ApplicationContext applicationContext) {
        this.supplierService = supplierService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        totalPurchasesCol.setCellValueFactory(new PropertyValueFactory<>("totalPurchases"));
        totalPaidCol.setCellValueFactory(new PropertyValueFactory<>("totalPaid"));
        balanceDueCol.setCellValueFactory(new PropertyValueFactory<>("balanceDue"));
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getStatus() != null ? data.getValue().getStatus().name() : ""));
        createdAtCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        if (paymentsTable != null) {
            paymentAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
            paymentMethodCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    resolvePaymentMethodDisplay(data.getValue())));
            paymentDateCol.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
            paymentNotesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

            formatMoneyColumn(paymentAmountCol);
            formatDateColumn(paymentDateCol);
        }

        formatMoneyColumn(totalPurchasesCol);
        formatMoneyColumn(totalPaidCol);
        formatMoneyColumn(balanceDueCol);
        formatDateColumn(createdAtCol);

        suppliersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                fillForm(selected);
                editingId = selected.getId();
                loadPaymentsForSupplier(selected);
            } else {
                clearPaymentsTable();
            }
        });

        loadData();
    }

    @FXML
    public void loadData() {
        List<Supplier> suppliers = supplierService.listAll(false);
        suppliersTable.setItems(FXCollections.observableArrayList(suppliers));
        clearPaymentsTable();
    }

    @FXML
    public void handleSearch() {
        String term = searchField.getText();
        if (term == null || term.isBlank()) {
            loadData();
            return;
        }
        suppliersTable.setItems(FXCollections.observableArrayList(
                supplierService.searchByNameOrPhone(term.trim())));
    }

    @FXML
    public void handleAddSupplier() {
        try {
            Supplier supplier = buildSupplierFromForm();
            supplier.setId(null);
            Supplier saved = supplierService.saveSupplier(supplier);
            clearForm();
            loadData();
            showSuccessWindow("تم تسجيل المورد بنجاح", "المورد: " + saved.getName());
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void showSuccessWindow(String message, String details) {
        if (successOverlay != null && successMessageLabel != null) {
            successMessageLabel.setText(message);
            if (successDetailsLabel != null) {
                successDetailsLabel.setText(details != null ? details : "");
            }
            successOverlay.setVisible(true);
            successOverlay.setManaged(true);
        }
    }

    @FXML
    public void handleCloseSuccess() {
        if (successOverlay != null) {
            successOverlay.setVisible(false);
            successOverlay.setManaged(false);
        }
    }

    @FXML
    public void handleEditSupplier() {
        Supplier selected = suppliersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("يرجى اختيار مورد للتعديل");
            return;
        }
        try {
            Supplier supplier = buildSupplierFromForm();
            supplier.setId(selected.getId());
            supplierService.saveSupplier(supplier);
            clearForm();
            loadData();
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    @FXML
    public void handleToggleStatus() {
        Supplier selected = suppliersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("يرجى اختيار مورد لتغيير الحالة");
            return;
        }
        SupplierStatus newStatus = selected.getStatus() == SupplierStatus.ACTIVE
                ? SupplierStatus.INACTIVE
                : SupplierStatus.ACTIVE;
        try {
            supplierService.updateStatus(selected.getId(), newStatus);
            loadData();
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    @FXML
    public void handleViewPurchases() {
        Supplier selected = suppliersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("يرجى اختيار مورد لعرض المشتريات");
            return;
        }
        showPurchasesDialog(selected);
    }

    @FXML
    public void handleRecordPayment() {
        // Populate the payment overlay
        if (paymentSupplierCombo != null) {
            paymentSupplierCombo.setItems(FXCollections.observableArrayList(supplierService.listAll(true)));
            Supplier selected = suppliersTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                paymentSupplierCombo.getSelectionModel().select(selected);
            }
        }
        if (paymentMethodCombo != null && paymentMethodCombo.getItems().isEmpty()) {
            paymentMethodCombo.setItems(FXCollections.observableArrayList("Cash", "InstaPay", "Visa", "Vodafone Cash"));
            paymentMethodCombo.getSelectionModel().selectFirst();
        }
        if (paymentAmountField != null)
            paymentAmountField.clear();
        if (paymentNotesField != null)
            paymentNotesField.clear();
        showOverlay(paymentOverlay);
    }

    @FXML
    public void handleSavePayment() {
        try {
            Supplier supplier = paymentSupplierCombo.getSelectionModel().getSelectedItem();
            if (supplier == null) {
                showAlert("يرجى اختيار المورد");
                return;
            }
            BigDecimal amount = parseAmount(paymentAmountField.getText());
            SupplierPayment payment = new SupplierPayment();
            String selectedMethod = paymentMethodCombo.getSelectionModel().getSelectedItem();
            payment.setSupplier(supplier);
            payment.setAmount(amount);
            payment.setMethod(mapSupplierPaymentMethod(selectedMethod));
            payment.setMethodDisplay(selectedMethod);
            payment.setNotes(paymentNotesField.getText());
            supplierService.recordPayment(payment);
            hideOverlay(paymentOverlay);
            loadData();
            loadPaymentsForSupplier(supplier);
            showSuccessWindow("تم تسجيل دفعة المورد بنجاح", "المبلغ: " + amount);
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    @FXML
    public void handleClosePayment() {
        hideOverlay(paymentOverlay);
    }

    @FXML
    public void handleRefresh() {
        loadData();
    }

    @FXML
    public void handleMergeDuplicates() {
        try {
            supplierService.mergeDuplicates();
            loadData();
            showSuccessWindow("تم دمج المكرر بنجاح", "");
        } catch (Exception ex) {
            showAlert("خطأ أثناء الدمج: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private Supplier buildSupplierFromForm() {
        Supplier supplier = new Supplier();
        supplier.setName(nameField.getText());
        supplier.setPhone(phoneField.getText());
        supplier.setEmail(emailField.getText());
        supplier.setAddress(addressField.getText());
        supplier.setTotalPurchases(parseBigDecimal(totalPurchasesField != null ? totalPurchasesField.getText() : null));
        supplier.setStatus(statusCheckBox != null && statusCheckBox.isSelected()
                ? SupplierStatus.ACTIVE
                : SupplierStatus.INACTIVE);
        return supplier;
    }

    private void fillForm(Supplier supplier) {
        nameField.setText(supplier.getName());
        phoneField.setText(supplier.getPhone());
        emailField.setText(supplier.getEmail());
        addressField.setText(supplier.getAddress());
        if (totalPurchasesField != null) {
            totalPurchasesField.setText(formatMoney(supplier.getTotalPurchases()));
        }
        statusCheckBox.setSelected(supplier.getStatus() == SupplierStatus.ACTIVE);
        updateTotalsLabels(supplier);
    }

    private void clearForm() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        addressField.clear();
        if (totalPurchasesField != null) {
            totalPurchasesField.clear();
        }
        if (statusCheckBox != null) {
            statusCheckBox.setSelected(true);
        }
        editingId = null;
        updateTotalsLabels(null);
    }

    private void updateTotalsLabels(Supplier supplier) {
        if (totalPurchasesLabel == null || totalPaidLabel == null || balanceDueLabel == null) {
            return;
        }
        java.math.BigDecimal totalPurchases = supplier != null ? supplier.getTotalPurchases() : null;
        java.math.BigDecimal totalPaid = supplier != null ? supplier.getTotalPaid() : null;
        java.math.BigDecimal balanceDue = supplier != null ? supplier.getBalanceDue() : null;
        totalPurchasesLabel.setText(formatMoney(totalPurchases));
        totalPaidLabel.setText(formatMoney(totalPaid));
        balanceDueLabel.setText(formatMoney(balanceDue));
    }

    private String formatMoney(java.math.BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return String.format("%.2f", value);
    }

    private void showOverlay(VBox overlay) {
        if (overlay != null) {
            overlay.setVisible(true);
            overlay.setManaged(true);
        }
    }

    private void hideOverlay(VBox overlay) {
        if (overlay != null) {
            overlay.setVisible(false);
            overlay.setManaged(false);
        }
    }

    private PaymentMethod mapSupplierPaymentMethod(String method) {
        if (method == null) {
            return PaymentMethod.CASH;
        }
        return switch (method) {
            case "Cash" -> PaymentMethod.CASH;
            case "InstaPay" -> PaymentMethod.INSTAPAY;
            case "Visa" -> PaymentMethod.VISA;
            case "Vodafone Cash" -> PaymentMethod.VODAFONE_CASH;
            default -> PaymentMethod.CASH;
        };
    }

    private String resolvePaymentMethodDisplay(SupplierPayment payment) {
        if (payment == null) {
            return "";
        }
        String display = payment.getMethodDisplay();
        if (display != null && !display.isBlank()) {
            return display;
        }
        PaymentMethod method = payment.getMethod();
        if (method == null) {
            return "";
        }
        return switch (method) {
            case CASH -> "Cash";
            case BANK -> "Bank";
            default -> "Other";
        };
    }

    private void showPurchasesDialog(Supplier supplier) {
        TableView<Purchase> table = new TableView<>();
        TableColumn<Purchase, BigDecimal> totalCol = new TableColumn<>("الإجمالي");
        TableColumn<Purchase, BigDecimal> paidCol = new TableColumn<>("المدفوع");
        TableColumn<Purchase, String> statusCol = new TableColumn<>("الحالة");
        TableColumn<Purchase, LocalDateTime> dateCol = new TableColumn<>("التاريخ");
        TableColumn<Purchase, String> notesCol = new TableColumn<>("ملاحظات");

        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        paidCol.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getPaymentStatus() != null ? data.getValue().getPaymentStatus().name() : ""));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

        formatMoneyColumn(totalCol);
        formatMoneyColumn(paidCol);
        formatDateColumn(dateCol);

        table.getColumns().addAll(totalCol, paidCol, statusCol, dateCol, notesCol);
        table.getItems().setAll(supplierService.listPurchases(supplier.getId()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TextField totalField = new TextField();
        totalField.setPromptText("الإجمالي");
        TextField paidField = new TextField();
        paidField.setPromptText("المدفوع");
        TextField notesField = new TextField();
        notesField.setPromptText("ملاحظات");
        Button addBtn = new Button("إضافة شراء");
        addBtn.setOnAction(e -> {
            try {
                BigDecimal total = parseAmount(totalField.getText());
                BigDecimal paid = parseAmount(paidField.getText());
                Purchase purchase = new Purchase();
                purchase.setSupplier(supplier);
                purchase.setTotalAmount(total);
                purchase.setPaidAmount(paid);
                purchase.setNotes(notesField.getText());
                supplierService.recordPurchase(purchase);
                table.getItems().setAll(supplierService.listPurchases(supplier.getId()));
                totalField.clear();
                paidField.clear();
                notesField.clear();
                loadData();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        HBox form = new HBox(8, totalField, paidField, notesField, addBtn);

        VBox content = new VBox(12, table, form);
        content.setStyle(
                "-fx-background-color: #0f172a; -fx-padding: 20; -fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");
        content.setMaxWidth(720);
        content.setMaxHeight(480);

        // Title row with close button
        HBox titleRow = new HBox();
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label titleLabel = new Label("مشتريات المورد: " + supplier.getName());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        javafx.scene.layout.HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-cursor: hand;");

        VBox cardWithTitle = new VBox(10, titleRow, content);
        cardWithTitle.setStyle(
                "-fx-background-color: #0f172a; -fx-padding: 20; -fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");
        cardWithTitle.setMaxWidth(720);
        cardWithTitle.setMaxHeight(520);

        VBox overlay = new VBox();
        overlay.setAlignment(javafx.geometry.Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

        titleRow.getChildren().addAll(titleLabel, closeBtn);
        closeBtn.setOnAction(e -> {
            if (nameField.getScene() != null
                    && nameField.getScene().getRoot() instanceof javafx.scene.layout.StackPane sp) {
                sp.getChildren().remove(overlay);
            }
        });

        overlay.getChildren().add(cardWithTitle);

        // Add to root StackPane
        if (nameField.getScene() != null
                && nameField.getScene().getRoot() instanceof javafx.scene.layout.StackPane sp) {
            sp.getChildren().add(overlay);
        }
    }

    private void loadPaymentsForSupplier(Supplier supplier) {
        if (paymentsTable == null) {
            return;
        }
        if (supplier == null || supplier.getId() == null) {
            clearPaymentsTable();
            return;
        }
        paymentsTable.setItems(FXCollections.observableArrayList(
                supplierService.listPayments(supplier.getId())));
    }

    private void clearPaymentsTable() {
        if (paymentsTable != null) {
            paymentsTable.getItems().clear();
        }
    }

    private <S> void formatMoneyColumn(TableColumn<S, BigDecimal> column) {
        column.setCellFactory(col -> new TableCell<S, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%.2f", item));
            }
        });
    }

    private <S> void formatDateColumn(TableColumn<S, LocalDateTime> column) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        column.setCellFactory(col -> new TableCell<S, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(formatter));
            }
        });
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        String normalized = value.trim().replace(",", "");
        return new BigDecimal(normalized);
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        String normalized = value.trim().replace(",", "");
        return new BigDecimal(normalized);
    }

    private void showAlert(String message) {
        if (errorOverlay != null && errorMessageLabel != null) {
            errorMessageLabel.setText(message);
            showOverlay(errorOverlay);
        }
    }

    @FXML
    public void handleCloseError() {
        hideOverlay(errorOverlay);
    }
}
