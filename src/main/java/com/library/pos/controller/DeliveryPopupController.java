package com.library.pos.controller;

import com.library.pos.model.Customer;
import com.library.pos.service.CustomerService;
import com.library.pos.util.DialogUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Scope;

import java.util.List;

@Controller
@Scope("prototype")
public class DeliveryPopupController {

    @FXML
    private TextField searchField;
    @FXML
    private ListView<Customer> customerListView;
    @FXML
    private Button selectBtn;

    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextArea addressArea;
    @FXML
    private Button saveBtn;

    private final CustomerService customerService;
    private final ObservableList<Customer> customerListModel = FXCollections.observableArrayList();

    // Result property to return selected customer
    private final ObjectProperty<Customer> selectedCustomer = new SimpleObjectProperty<>();

    public DeliveryPopupController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @FXML
    public void initialize() {
        customerListView.setItems(customerListModel);
        customerListView.setCellFactory(lv -> new ListCell<>() {
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
                    if (!phone.isBlank()) {
                        display = display + " - " + phone;
                    }
                    if (!code.isBlank()) {
                        display = display + " (كود: " + code + ")";
                    }
                    setText(display);
                }
            }
        });

        customerListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            selectBtn.setDisable(!hasSelection);

            if (hasSelection) {
                // Populate form for editing/confirmation
                populateForm(newVal);
            }
        });

        // Auto-search if typing phone number
        searchField.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.length() >= 3) {
                handleSearch();
            }
        });
    }

    private void populateForm(Customer c) {
        nameField.setText(c.getCustomerName());
        phoneField.setText(c.getMobile());
        addressArea.setText(c.getAddress());
        // Enable editing to update address if needed
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        if (query == null || query.isBlank()) {
            customerListModel.clear();
            return;
        }

        List<Customer> results = customerService.searchCustomers(query.trim());
        customerListModel.setAll(results);
    }

    @FXML
    private void handleSelectCustomer() {
        Customer selected = customerListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Check if address needs update from form
            if (addressArea.getText() != null && !addressArea.getText().trim().equals(selected.getAddress())) {
                selected.setAddress(addressArea.getText().trim());
                customerService.saveCustomer(selected);
            }
            selectedCustomer.set(selected);
            closeWindow();
        }
    }

    @FXML
    private void handleSaveAndSelect() {
        String name = nameField.getText();
        String phone = phoneField.getText();
        String address = addressArea.getText();

        if (name == null || name.isBlank()) {
            showAlert("خطأ", "يجب إدخال اسم العميل");
            return;
        }
        if (phone == null || phone.isBlank()) {
            showAlert("خطأ", "يجب إدخال رقم الهاتف");
            return;
        }

        // Check if creating new or updating existing selection
        Customer customer = customerListView.getSelectionModel().getSelectedItem();

        // If no customer selected, create new
        if (customer == null) {
            // Check if phone already exists
            List<Customer> existing = customerService.searchCustomers(phone.trim());
            if (!existing.isEmpty()) {
                // Try to match exact phone
                customer = existing.stream()
                        .filter(c -> c.getMobile().equals(phone.trim()))
                        .findFirst().orElse(null);
            }
        }

        if (customer == null) {
            customer = new Customer();
        }

        customer.setCustomerName(name.trim());
        customer.setMobile(phone.trim());
        customer.setAddress(address != null ? address.trim() : "");

        try {
            Customer saved = customerService.saveCustomer(customer);
            selectedCustomer.set(saved);
            if (saved.getCustomerCode() != null && !saved.getCustomerCode().isBlank()) {
                showAlert("تم الحفظ", "كود العميل: " + saved.getCustomerCode());
            }
            closeWindow();
        } catch (Exception e) {
            showAlert("Error", "فشل حفظ العميل: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearForm() {
        nameField.clear();
        phoneField.clear();
        addressArea.clear();
        customerListView.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleClose() {
        selectedCustomer.set(null);
        closeWindow();
    }

    private void closeWindow() {
        if (searchField.getScene() != null && searchField.getScene().getWindow() instanceof Stage) {
            ((Stage) searchField.getScene().getWindow()).close();
        }
    }

    public ObjectProperty<Customer> selectedCustomerProperty() {
        return selectedCustomer;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        DialogUtil.initOwner(alert, searchField.getScene() != null ? searchField.getScene().getWindow() : null);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
