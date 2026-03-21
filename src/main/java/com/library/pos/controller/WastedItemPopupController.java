package com.library.pos.controller;

import org.springframework.stereotype.Component;

import com.library.pos.model.Product;
import com.library.pos.model.User;
import com.library.pos.service.ProductService;
import com.library.pos.service.WastedItemService;

import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

@Component
public class WastedItemPopupController {

    @FXML private TextField     productSearchField;
    @FXML private ScrollPane    productScrollPane;
    @FXML private VBox          productListContainer;
    @FXML private HBox          productInfoBox;
    @FXML private Label         selectedProductNameLabel;
    @FXML private Label         selectedProductBarcodeLabel;
    @FXML private Label         selectedProductStockLabel;
    @FXML private Label         selectedProductCostLabel;

    @FXML private HBox          quantityBox;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Label         totalLossLabel;

    @FXML private VBox          notesBox;
    @FXML private TextArea      notesArea;

    @FXML private Label         messageLabel;
    @FXML private Button        confirmButton;

    private final ProductService     productService;
    private final WastedItemService  wastedItemService;

    private Product selectedProduct;
    private User    currentWorker;
    private Runnable onDoneCallback;

    private PauseTransition searchPause;

    public WastedItemPopupController(ProductService productService, WastedItemService wastedItemService) {
        this.productService    = productService;
        this.wastedItemService = wastedItemService;
    }

    @FXML
    private void initialize() {
        SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1);
        quantitySpinner.setValueFactory(factory);
        quantitySpinner.valueProperty().addListener((obs, oldV, newV) -> updateTotalLoss());

        searchPause = new PauseTransition(Duration.millis(300));
        searchPause.setOnFinished(e -> doSearch());

        // Allow Enter in search field to trigger search
        productSearchField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                searchPause.stop();
                doSearch();
            }
        });
    }

    // ── Called from CashierController ─────────────────────────────────────────
    public void setWorker(User worker)        { this.currentWorker = worker; }
    public void setOnDoneCallback(Runnable r) { this.onDoneCallback = r; }

    // ── Product Search ─────────────────────────────────────────────────────────
    @FXML
    private void handleProductSearch(KeyEvent e) {
        searchPause.playFromStart();
    }

    // Overload for the button click (no KeyEvent param)
    @FXML
    private void handleProductSearch() {
        searchPause.stop();
        doSearch();
    }

    private void doSearch() {
        String term = productSearchField.getText();
        if (term == null || term.isBlank()) {
            hideProductList();
            return;
        }
        List<Product> results = productService.searchByBarcodeOrName(term.trim());
        showProductList(results);
    }

    private void showProductList(List<Product> products) {
        productListContainer.getChildren().clear();
        if (products.isEmpty()) {
            Label empty = new Label("لا توجد نتائج");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 4;");
            productListContainer.getChildren().add(empty);
            productScrollPane.setVisible(true);
            productScrollPane.setManaged(true);
            return;
        }
        for (Product p : products) {
            Button btn = new Button(p.getName() + "   [" + p.getBarcode() + "]   (مخزون: " + p.getQuantity() + ")");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER_RIGHT; -fx-font-family: 'Cairo';"
                    + "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6 10;");
            btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-background-color: #f1f5f9;"));
            btn.setOnMouseExited(e -> btn.setStyle(
                    "-fx-background-color: transparent; -fx-alignment: CENTER_RIGHT; -fx-font-family: 'Cairo';"
                            + "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6 10;"));
            btn.setOnAction(e -> selectProduct(p));
            productListContainer.getChildren().add(btn);
        }
        productScrollPane.setVisible(true);
        productScrollPane.setManaged(true);
    }

    private void hideProductList() {
        productScrollPane.setVisible(false);
        productScrollPane.setManaged(false);
    }

    private void selectProduct(Product product) {
        this.selectedProduct = product;
        hideProductList();
        productSearchField.setText(product.getName());

        // Show product info
        selectedProductNameLabel.setText(product.getName());
        selectedProductBarcodeLabel.setText("باركود: " + product.getBarcode());
        selectedProductStockLabel.setText("المخزون الحالي: " + product.getQuantity());
        selectedProductCostLabel.setText(String.format("%.2f ج.م", product.getCost()));
        productInfoBox.setVisible(true);
        productInfoBox.setManaged(true);

        // Reset spinner max to current stock
        int maxQty = Math.max(1, product.getQuantity());
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxQty, 1));
        quantitySpinner.valueProperty().addListener((obs, o, n) -> updateTotalLoss());
        updateTotalLoss();

        quantityBox.setVisible(true);
        quantityBox.setManaged(true);
        notesBox.setVisible(true);
        notesBox.setManaged(true);
        confirmButton.setVisible(true);
        confirmButton.setManaged(true);
        hideMessage();
    }

    // ── Quantity Buttons ───────────────────────────────────────────────────────
    @FXML
    private void handleQuantityMinus() {
        int current = quantitySpinner.getValue();
        if (current > 1) quantitySpinner.getValueFactory().setValue(current - 1);
    }

    @FXML
    private void handleQuantityPlus() {
        int current = quantitySpinner.getValue();
        int max = selectedProduct != null ? selectedProduct.getQuantity() : 9999;
        if (current < max) quantitySpinner.getValueFactory().setValue(current + 1);
    }

    private void updateTotalLoss() {
        if (selectedProduct == null) return;
        int qty = quantitySpinner.getValue() != null ? quantitySpinner.getValue() : 1;
        double loss = selectedProduct.getCost() * qty;
        totalLossLabel.setText(String.format("%.2f ج.م", loss));
    }

    // ── Confirm ────────────────────────────────────────────────────────────────
    @FXML
    private void handleConfirm() {
        if (selectedProduct == null) {
            showError("يرجى اختيار منتج أولاً");
            return;
        }
        int qty = quantitySpinner.getValue() != null ? quantitySpinner.getValue() : 1;
        String notes = notesArea.getText();

        try {
            wastedItemService.recordWaste(selectedProduct, qty, currentWorker, notes);
            showSuccess("تم تسجيل " + qty + " قطعة من «" + selectedProduct.getName() + "» كهالك");
            // Refresh and close after a moment
            if (onDoneCallback != null) onDoneCallback.run();
            Platform.runLater(() -> {
                PauseTransition delay = new PauseTransition(Duration.seconds(1.4));
                delay.setOnFinished(e -> handleClose());
                delay.play();
            });
        } catch (IllegalStateException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("خطأ: " + ex.getMessage());
        }
    }

    // ── Close ──────────────────────────────────────────────────────────────────
    @FXML
    private void handleClose() {
        Stage stage = (Stage) productSearchField.getScene().getWindow();
        stage.close();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: #dc2626; -fx-background-color: #fef2f2; -fx-padding: 6; -fx-background-radius: 4;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: #16a34a; -fx-background-color: #f0fdf4; -fx-padding: 6; -fx-background-radius: 4;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }
}
