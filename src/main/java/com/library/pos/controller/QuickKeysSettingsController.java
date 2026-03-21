package com.library.pos.controller;

import com.library.pos.model.Product;
import com.library.pos.service.ProductService;
import com.library.pos.util.AppSettings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class QuickKeysSettingsController {

    @FXML private TextField f1Field;
    @FXML private TextField f2Field;
    @FXML private TextField f3Field;
    @FXML private TextField f4Field;
    @FXML private TextField f5Field;
    @FXML private TextField f6Field;

    @FXML private TextField f1Qty;
    @FXML private TextField f2Qty;
    @FXML private TextField f3Qty;
    @FXML private TextField f4Qty;
    @FXML private TextField f5Qty;
    @FXML private TextField f6Qty;

    @FXML private Label f1Name;
    @FXML private Label f2Name;
    @FXML private Label f3Name;
    @FXML private Label f4Name;
    @FXML private Label f5Name;
    @FXML private Label f6Name;

    private final ProductService productService;
    private CashierController parentController;

    // Optional Autowired implicit for Single constructor
    public QuickKeysSettingsController(ProductService productService) {
        this.productService = productService;
    }

    public void setParentController(CashierController parentController) {
        this.parentController = parentController;
    }

    @FXML
    public void initialize() {
        setupRow("1", f1Field, f1Qty, f1Name);
        setupRow("2", f2Field, f2Qty, f2Name);
        setupRow("3", f3Field, f3Qty, f3Name);
        setupRow("4", f4Field, f4Qty, f4Name);
        setupRow("5", f5Field, f5Qty, f5Name);
        setupRow("6", f6Field, f6Qty, f6Name);
    }

    private void setupRow(String keyNum, TextField field, TextField qtyField, Label nameLabel) {
        String existing = AppSettings.get("quick.F" + keyNum, "");
        String existingQty = AppSettings.get("quick.qty.F" + keyNum, "1");
        
        field.setText(existing);
        qtyField.setText(existingQty);
        updateLabelIfValid(existing, nameLabel);

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // focus lost
                updateLabelIfValid(field.getText() != null ? field.getText().trim() : "", nameLabel);
            }
        });

        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                nameLabel.setText("");
            }
        });
        
        field.setOnAction(e -> {
            updateLabelIfValid(field.getText() != null ? field.getText().trim() : "", nameLabel);
        });
        
        // Ensure quantity only receives numbers
        qtyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*(\\.\\d*)?")) {
                qtyField.setText(newVal.replaceAll("[^\\d.]", ""));
            }
        });
    }

    private void updateLabelIfValid(String barcode, Label nameLabel) {
        if (barcode == null || barcode.trim().isEmpty()) {
            nameLabel.setText("");
            return;
        }
        Optional<Product> prod = productService.findByBarcode(barcode);
        if (prod.isPresent()) {
            nameLabel.setText(prod.get().getName());
            nameLabel.setStyle("-fx-text-fill: green;");
        } else {
            nameLabel.setText("غير موجود");
            nameLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleSave() {
        saveRow("1", f1Field, f1Qty);
        saveRow("2", f2Field, f2Qty);
        saveRow("3", f3Field, f3Qty);
        saveRow("4", f4Field, f4Qty);
        saveRow("5", f5Field, f5Qty);
        saveRow("6", f6Field, f6Qty);

        if (parentController != null) {
            parentController.reloadQuickKeys();
        }
        closeStage();
    }

    private void saveRow(String keyNum, TextField field, TextField qtyField) {
        String barcode = field.getText() != null ? field.getText().trim() : "";
        String qtyStr = qtyField.getText() != null ? qtyField.getText().trim() : "1";
        if (qtyStr.isEmpty()) qtyStr = "1";
        
        AppSettings.set("quick.F" + keyNum, barcode);
        AppSettings.set("quick.qty.F" + keyNum, qtyStr);
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private void closeStage() {
        if (f1Field != null && f1Field.getScene() != null && f1Field.getScene().getWindow() instanceof Stage) {
            ((Stage) f1Field.getScene().getWindow()).close();
        }
    }
}
