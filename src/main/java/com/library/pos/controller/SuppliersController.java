package com.library.pos.controller;

import com.library.pos.model.PaymentMethod;
import com.library.pos.model.Purchase;
import com.library.pos.model.Supplier;
import com.library.pos.model.SupplierPayment;
import com.library.pos.model.SupplierStatus;
import com.library.pos.service.SupplierService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
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

    private final SupplierService supplierService;
    private Long editingId;

    public SuppliersController(SupplierService supplierService) {
        this.supplierService = supplierService;
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

        formatMoneyColumn(totalPurchasesCol);
        formatMoneyColumn(totalPaidCol);
        formatMoneyColumn(balanceDueCol);
        formatDateColumn(createdAtCol);

        suppliersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                fillForm(selected);
                editingId = selected.getId();
            }
        });

        loadData();
    }

    @FXML
    public void loadData() {
        List<Supplier> suppliers = supplierService.listAll(false);
        suppliersTable.setItems(FXCollections.observableArrayList(suppliers));
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
            supplierService.saveSupplier(supplier);
            clearForm();
            loadData();
        } catch (Exception ex) {
            showAlert(ex.getMessage());
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
        showPaymentDialog();
    }

    @FXML
    public void handleRefresh() {
        loadData();
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

    private void showPaymentDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("تسجيل دفعة للمورد");

        VBox root = new VBox(12);
        root.setStyle("-fx-padding: 16; -fx-background-color: #0f172a;");

        ComboBox<Supplier> supplierCombo = new ComboBox<>();
        supplierCombo.setItems(FXCollections.observableArrayList(supplierService.listAll(true)));
        supplierCombo.setPromptText("اختر المورد");

        Supplier selected = suppliersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            supplierCombo.getSelectionModel().select(selected);
        }

        TextField amountField = new TextField();
        amountField.setPromptText("المبلغ");

        ComboBox<PaymentMethod> methodCombo = new ComboBox<>();
        methodCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        methodCombo.getSelectionModel().select(PaymentMethod.CASH);

        TextArea notesField = new TextArea();
        notesField.setPromptText("ملاحظات");
        notesField.setPrefRowCount(2);

        Button saveBtn = new Button("تسجيل");
        saveBtn.setOnAction(e -> {
            try {
                Supplier supplier = supplierCombo.getSelectionModel().getSelectedItem();
                if (supplier == null) {
                    showAlert("يرجى اختيار المورد");
                    return;
                }
                BigDecimal amount = parseAmount(amountField.getText());
                SupplierPayment payment = new SupplierPayment();
                payment.setSupplier(supplier);
                payment.setAmount(amount);
                payment.setMethod(methodCombo.getSelectionModel().getSelectedItem());
                payment.setNotes(notesField.getText());
                supplierService.recordPayment(payment);
                dialog.close();
                loadData();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        root.getChildren().addAll(
                new Label("المورد"), supplierCombo,
                new Label("المبلغ"), amountField,
                new Label("طريقة الدفع"), methodCombo,
                new Label("ملاحظات"), notesField,
                saveBtn);

        Scene scene = new Scene(root, 360, 420);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showPurchasesDialog(Supplier supplier) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("مشتريات المورد: " + supplier.getName());

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

        VBox root = new VBox(12, table, form);
        root.setStyle("-fx-padding: 16; -fx-background-color: #0f172a;");

        Scene scene = new Scene(root, 700, 420);
        dialog.setScene(scene);
        dialog.showAndWait();
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }
}
