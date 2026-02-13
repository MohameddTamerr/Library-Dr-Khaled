package com.library.pos.controller;

import com.library.pos.model.Product;
import com.library.pos.model.User;
import com.library.pos.service.ProductService;
import com.library.pos.service.SaleService;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReturnsPopupController {

    @FXML
    private TextField productSearchField;

    @FXML
    private ScrollPane productScrollPane;

    @FXML
    private VBox productCheckboxContainer;

    @FXML
    private HBox productDetailsBox;

    @FXML
    private Label selectedProductName;

    @FXML
    private Label selectedProductBarcode;

    @FXML
    private Label selectedProductPrice;

    @FXML
    private HBox quantitySection;

    @FXML
    private Spinner<Integer> quantitySpinner;

    @FXML
    private VBox conditionSection;

    @FXML
    private RadioButton goodConditionRadio;

    @FXML
    private RadioButton badConditionRadio;

    @FXML
    private ToggleGroup conditionGroup;

    @FXML
    private HBox totalSection;

    @FXML
    private Label totalLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private Button confirmButton;
    @FXML
    private Button cancelButton;

    private final ProductService productService;
    private final SaleService saleService;

    private Product selectedProduct;
    private User currentWorker;
    private Runnable onReturnProcessedCallback;
    private Map<CheckBox, Product> checkboxProductMap = new HashMap<>();
    private final List<CheckBox> productCheckBoxes = new ArrayList<>();

    public ReturnsPopupController(ProductService productService, SaleService saleService) {
        this.productService = productService;
        this.saleService = saleService;
    }

    @FXML
    private void initialize() {
        // Setup quantity spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1);
        quantitySpinner.setValueFactory(valueFactory);

        // Add listener to quantity changes
        quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTotal());

        if (confirmButton != null) {
            confirmButton.setDefaultButton(true);
        }
        if (cancelButton != null) {
            cancelButton.setCancelButton(true);
        }

        productSearchField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DOWN && !productCheckBoxes.isEmpty()) {
                productCheckBoxes.get(0).requestFocus();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER && productCheckBoxes.size() == 1) {
                CheckBox only = productCheckBoxes.get(0);
                only.setSelected(true);
                selectProduct(checkboxProductMap.get(only));
                event.consume();
            }
        });

        // Focus on search field
        Platform.runLater(() -> productSearchField.requestFocus());
    }

    @FXML
    private void handleProductSearch(KeyEvent event) {
        handleProductSearch();
    }

    @FXML
    private void handleProductSearch() {
        String query = productSearchField.getText().trim();

        if (query.isEmpty()) {
            productScrollPane.setVisible(false);
            productScrollPane.setManaged(false);
            return;
        }

        // Search for products using the built-in search method
        List<Product> matchingProducts = productService.searchByBarcodeOrName(query);

        if (matchingProducts.isEmpty()) {
            productScrollPane.setVisible(false);
            productScrollPane.setManaged(false);
            showMessage("لم يتم العثور على منتجات", "error");
        } else {
            // Clear previous checkboxes
            productCheckboxContainer.getChildren().clear();
            checkboxProductMap.clear();
            productCheckBoxes.clear();

            // Create checkboxes for each matching product
            for (int i = 0; i < matchingProducts.size(); i++) {
                Product product = matchingProducts.get(i);
                CheckBox checkBox = new CheckBox();
                checkBox.setText(String.format("%s  |  باركود: %s  |  %.2f ج.م",
                        product.getName(),
                        product.getBarcode(),
                        product.getSellPrice()));
                checkBox.setStyle("-fx-font-size: 13px; -fx-padding: 6 0;");
                checkBox.setWrapText(true);

                // Store mapping
                checkboxProductMap.put(checkBox, product);

                // Handle checkbox selection - only allow one selection
                checkBox.setOnAction(e -> {
                    if (checkBox.isSelected()) {
                        // Uncheck all others
                        for (CheckBox cb : checkboxProductMap.keySet()) {
                            if (cb != checkBox) {
                                cb.setSelected(false);
                            }
                        }
                        selectProduct(product);
                    } else {
                        // Clear selection
                        clearProductSelection();
                    }
                });
                final int idx = i;
                checkBox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.DOWN && idx < productCheckBoxes.size() - 1) {
                        productCheckBoxes.get(idx + 1).requestFocus();
                        event.consume();
                    } else if (event.getCode() == KeyCode.UP && idx > 0) {
                        productCheckBoxes.get(idx - 1).requestFocus();
                        event.consume();
                    } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                        checkBox.setSelected(true);
                        selectProduct(product);
                        event.consume();
                    }
                });

                productCheckboxContainer.getChildren().add(checkBox);
                productCheckBoxes.add(checkBox);
            }

            productScrollPane.setVisible(true);
            productScrollPane.setManaged(true);
            hideMessage();
        }
    }

    private void selectProduct(Product product) {
        this.selectedProduct = product;

        // Show product details
        selectedProductName.setText(product.getName());
        selectedProductBarcode.setText("الباركود: " + product.getBarcode());
        selectedProductPrice.setText(String.format("%.2f ج.م", product.getSellPrice()));

        productDetailsBox.setVisible(true);
        productDetailsBox.setManaged(true);

        // Show quantity section (includes condition)
        quantitySection.setVisible(true);
        quantitySection.setManaged(true);

        totalSection.setVisible(true);
        totalSection.setManaged(true);

        // Enable confirm button
        confirmButton.setDisable(false);

        // Update total
        updateTotal();

        // Clear any previous messages
        hideMessage();
    }

    private void clearProductSelection() {
        this.selectedProduct = null;

        productDetailsBox.setVisible(false);
        productDetailsBox.setManaged(false);

        quantitySection.setVisible(false);
        quantitySection.setManaged(false);

        totalSection.setVisible(false);
        totalSection.setManaged(false);

        confirmButton.setDisable(true);
    }

    @FXML
    private void handleQuantityMinus() {
        int current = quantitySpinner.getValue();
        if (current > 1) {
            quantitySpinner.getValueFactory().setValue(current - 1);
        }
    }

    @FXML
    private void handleQuantityPlus() {
        int current = quantitySpinner.getValue();
        if (current < 999) {
            quantitySpinner.getValueFactory().setValue(current + 1);
        }
    }

    private void updateTotal() {
        if (selectedProduct != null) {
            int quantity = quantitySpinner.getValue();
            double total = selectedProduct.getSellPrice() * quantity;
            totalLabel.setText(String.format("%.2f ج.م", total));
        }
    }

    @FXML
    private void handleConfirmReturn() {
        if (selectedProduct == null) {
            showMessage("يرجى اختيار منتج أولاً", "error");
            return;
        }

        if (currentWorker == null) {
            showMessage("خطأ: لم يتم تحديد الموظف", "error");
            return;
        }

        try {
            int quantityToReturn = quantitySpinner.getValue();
            String condition = goodConditionRadio.isSelected() ? "GOOD" : "BAD";

            // Process the return
            saleService.processProductReturn(selectedProduct, quantityToReturn, condition, currentWorker);

            // Show success message
            showMessage("تم إرجاع المنتج بنجاح ✓", "success");

            // Call callback if set
            if (onReturnProcessedCallback != null) {
                onReturnProcessedCallback.run();
            }

            PauseTransition delay = new PauseTransition(Duration.millis(700));
            delay.setOnFinished(e -> handleClose());
            delay.play();

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("خطأ في معالجة الإرجاع: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) productSearchField.getScene().getWindow();
        stage.close();
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        if ("success".equals(type)) {
            messageLabel.setStyle(
                    "-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-padding: 8; -fx-background-radius: 6;");
        } else {
            messageLabel.setStyle(
                    "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 8; -fx-background-radius: 6;");
        }
    }

    private void hideMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    // Setters for dependencies
    public void setCurrentWorker(User worker) {
        this.currentWorker = worker;
    }

    public void setOnReturnProcessed(Runnable callback) {
        this.onReturnProcessedCallback = callback;
    }
}
