package com.library.pos.controller;

import com.library.pos.model.Customer;
import com.library.pos.model.CustomerDeferredPayment;
import com.library.pos.model.CustomerDeferredSummary;
import com.library.pos.model.PaymentMethod;
import com.library.pos.service.CustomerDeferredService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.List;

@Component
public class DeferredPaymentsController {

    @FXML
    private TextField searchField;
    @FXML
    private Label totalDeferredLabel;
    @FXML
    private Label totalPaidLabel;
    @FXML
    private Label balanceDueLabel;

    @FXML
    private TableView<CustomerDeferredSummary> customersTable;
    @FXML
    private TableColumn<CustomerDeferredSummary, String> customerNameCol;
    @FXML
    private TableColumn<CustomerDeferredSummary, String> customerCodeCol;
    @FXML
    private TableColumn<CustomerDeferredSummary, String> mobileCol;
    @FXML
    private TableColumn<CustomerDeferredSummary, BigDecimal> totalDeferredCol;
    @FXML
    private TableColumn<CustomerDeferredSummary, BigDecimal> totalPaidCol;
    @FXML
    private TableColumn<CustomerDeferredSummary, BigDecimal> balanceCol;
    @FXML
    private TableColumn<CustomerDeferredSummary, String> statusCol;

    @FXML
    private TableView<CustomerDeferredPayment> paymentsTable;
    @FXML
    private TableColumn<CustomerDeferredPayment, BigDecimal> paymentAmountCol;
    @FXML
    private TableColumn<CustomerDeferredPayment, PaymentMethod> paymentMethodCol;
    @FXML
    private TableColumn<CustomerDeferredPayment, String> paymentDateCol;
    @FXML
    private TableColumn<CustomerDeferredPayment, String> paymentNotesCol;

    @FXML
    private VBox paymentOverlay;
    @FXML
    private ComboBox<CustomerDeferredSummary> paymentCustomerCombo;
    @FXML
    private TextField paymentAmountField;
    @FXML
    private ComboBox<String> paymentMethodCombo;
    @FXML
    private TextArea paymentNotesField;
    @FXML
    private VBox successOverlay;
    @FXML
    private Label successMessageLabel;
    @FXML
    private Label successDetailsLabel;
    @FXML
    private VBox errorOverlay;
    @FXML
    private Label errorMessageLabel;

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final CustomerDeferredService deferredService;

    public DeferredPaymentsController(CustomerDeferredService deferredService) {
        this.deferredService = deferredService;
    }

    @FXML
    public void initialize() {
        customerNameCol.setCellValueFactory(data -> new SimpleStringProperty(getCustomerName(data.getValue())));
        customerCodeCol.setCellValueFactory(data -> new SimpleStringProperty(getCustomerCode(data.getValue())));
        mobileCol.setCellValueFactory(data -> new SimpleStringProperty(getCustomerMobile(data.getValue())));
        totalDeferredCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getTotalDeferred()));
        totalPaidCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getTotalPaid()));
        balanceCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getBalanceDue()));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(toArabicStatus(data.getValue())));

        formatBigDecimalColumn(totalDeferredCol);
        formatBigDecimalColumn(totalPaidCol);
        formatBigDecimalColumn(balanceCol);

        paymentAmountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getAmount()));
        paymentMethodCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("method"));
        paymentDateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPaymentDate() != null ? data.getValue().getPaymentDate().format(DATE_TIME_FORMAT) : ""));
        paymentNotesCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getNotes() != null ? data.getValue().getNotes() : ""));

        formatBigDecimalColumn(paymentAmountCol);

        customersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            updateSummaryCards(selected);
            loadPaymentsForSelectedCustomer(selected);
        });

        if (paymentMethodCombo != null) {
            paymentMethodCombo.setItems(FXCollections.observableArrayList("Cash", "InstaPay", "Visa", "Vodafone Cash"));
            paymentMethodCombo.getSelectionModel().selectFirst();
        }
        if (paymentCustomerCombo != null) {
            paymentCustomerCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(CustomerDeferredSummary summary) {
                    if (summary == null || summary.getCustomer() == null) {
                        return "";
                    }
                    Customer c = summary.getCustomer();
                    return c.getCustomerName() + " (" + (c.getCustomerCode() != null ? c.getCustomerCode() : "-") + ")";
                }

                @Override
                public CustomerDeferredSummary fromString(String string) {
                    return null;
                }
            });
        }
        loadData();
    }

    @FXML
    private void handleSearch() {
        loadData();
    }

    @FXML
    private void handleRefresh() {
        if (searchField != null) {
            searchField.clear();
        }
        loadData();
    }

    @FXML
    private void handleRecordPayment() {
        if (paymentCustomerCombo != null) {
            paymentCustomerCombo.setItems(customersTable.getItems());
            CustomerDeferredSummary selected = customersTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                paymentCustomerCombo.getSelectionModel().select(selected);
            }
        }
        if (paymentAmountField != null) {
            paymentAmountField.clear();
        }
        if (paymentNotesField != null) {
            paymentNotesField.clear();
        }
        showOverlay(paymentOverlay);
    }

    @FXML
    private void handleSavePayment() {
        try {
            CustomerDeferredSummary selected = paymentCustomerCombo.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getCustomer() == null || selected.getCustomer().getId() == null) {
                showError("يرجى اختيار العميل");
                return;
            }
            String amountText = paymentAmountField != null ? paymentAmountField.getText() : null;
            BigDecimal amount = parseAmount(amountText);

            CustomerDeferredPayment payment = new CustomerDeferredPayment();
            payment.setCustomer(selected.getCustomer());
            payment.setAmount(amount);
            payment.setMethod(mapPaymentMethod(paymentMethodCombo != null ? paymentMethodCombo.getValue() : null));
            payment.setNotes(paymentNotesField != null ? paymentNotesField.getText() : null);
            deferredService.recordPayment(payment);

            hideOverlay(paymentOverlay);
            loadData();
            customersTable.getItems().stream()
                    .filter(s -> s.getCustomer() != null && s.getCustomer().getId().equals(selected.getCustomer().getId()))
                    .findFirst()
                    .ifPresent(s -> customersTable.getSelectionModel().select(s));
            showSuccess("تم تسجيل دفعة الآجل بنجاح", "المبلغ: " + amount);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleClosePayment() {
        hideOverlay(paymentOverlay);
    }

    @FXML
    private void handleCloseSuccess() {
        hideOverlay(successOverlay);
    }

    @FXML
    private void handleCloseError() {
        hideOverlay(errorOverlay);
    }

    private void loadData() {
        String term = searchField != null ? searchField.getText() : null;
        List<CustomerDeferredSummary> rows = deferredService.listCustomerSummaries(term);
        customersTable.setItems(FXCollections.observableArrayList(rows));
        if (!rows.isEmpty()) {
            customersTable.getSelectionModel().selectFirst();
        } else {
            updateSummaryCards(null);
            paymentsTable.getItems().clear();
        }
    }

    private void loadPaymentsForSelectedCustomer(CustomerDeferredSummary selected) {
        if (selected == null || selected.getCustomer() == null || selected.getCustomer().getId() == null) {
            paymentsTable.getItems().clear();
            return;
        }
        paymentsTable.setItems(FXCollections.observableArrayList(
                deferredService.listPayments(selected.getCustomer().getId())));
    }

    private void updateSummaryCards(CustomerDeferredSummary selected) {
        BigDecimal totalDeferred = selected != null ? selected.getTotalDeferred() : BigDecimal.ZERO;
        BigDecimal totalPaid = selected != null ? selected.getTotalPaid() : BigDecimal.ZERO;
        BigDecimal balance = selected != null ? selected.getBalanceDue() : BigDecimal.ZERO;
        totalDeferredLabel.setText(String.format("%.2f", totalDeferred));
        totalPaidLabel.setText(String.format("%.2f", totalPaid));
        balanceDueLabel.setText(String.format("%.2f", balance));
    }

    private String getCustomerName(CustomerDeferredSummary row) {
        return row != null && row.getCustomer() != null && row.getCustomer().getCustomerName() != null
                ? row.getCustomer().getCustomerName()
                : "";
    }

    private String getCustomerCode(CustomerDeferredSummary row) {
        return row != null && row.getCustomer() != null && row.getCustomer().getCustomerCode() != null
                ? row.getCustomer().getCustomerCode()
                : "";
    }

    private String getCustomerMobile(CustomerDeferredSummary row) {
        return row != null && row.getCustomer() != null && row.getCustomer().getMobile() != null
                ? row.getCustomer().getMobile()
                : "";
    }

    private String toArabicStatus(CustomerDeferredSummary row) {
        if (row == null || row.getPaymentStatus() == null) {
            return "";
        }
        return switch (row.getPaymentStatus()) {
            case PAID -> "مدفوع";
            case PARTIAL -> "جزئي";
            case UNPAID -> "غير مدفوع";
        };
    }

    private <S> void formatBigDecimalColumn(TableColumn<S, BigDecimal> column) {
        column.setCellFactory(col -> new javafx.scene.control.TableCell<S, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%.2f", item));
            }
        });
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("يرجى إدخال مبلغ صحيح");
        }
        String normalized = value.trim().replace(",", "");
        return new BigDecimal(normalized);
    }

    private PaymentMethod mapPaymentMethod(String method) {
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

    private void showSuccess(String message, String details) {
        if (successMessageLabel != null) {
            successMessageLabel.setText(message != null ? message : "");
        }
        if (successDetailsLabel != null) {
            successDetailsLabel.setText(details != null ? details : "");
        }
        showOverlay(successOverlay);
    }

    private void showError(String message) {
        if (errorMessageLabel != null) {
            errorMessageLabel.setText(message != null ? message : "حدث خطأ");
        }
        showOverlay(errorOverlay);
    }
}
