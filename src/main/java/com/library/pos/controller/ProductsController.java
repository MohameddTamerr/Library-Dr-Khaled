package com.library.pos.controller;

import com.library.pos.model.Product;
import com.library.pos.service.ProductService;
import com.library.pos.util.AutoRefreshUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import javafx.util.Callback;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

import java.util.ResourceBundle;

@Component
public class ProductsController {

    @FXML
    private TableView<Product> productsTable;
    @FXML
    private TableColumn<Product, String> nameCol;
    @FXML
    private TableColumn<Product, String> barcodeCol;
    @FXML
    private TableColumn<Product, String> categoryCol;
    @FXML
    private TableColumn<Product, Double> costCol;
    @FXML
    private TableColumn<Product, Double> sellPriceCol;
    @FXML
    private TableColumn<Product, Integer> qtyCol;
    @FXML
    private TableColumn<Product, Integer> minStockCol;

    @FXML
    private TableColumn<Product, Void> actionCol;

    @FXML
    private TextField barcodeSearchField;

    @FXML
    private ResourceBundle resources;

    private final ProductService productService;
    private Timeline autoRefreshTimeline;
    private static final int REFRESH_SECONDS_VISIBLE = 8;
    private static final int REFRESH_SECONDS_HIDDEN = 16;
    private LocalDateTime lastUpdatedAt;
    private long lastCount = -1;

    public ProductsController(ProductService productService) {
        this.productService = productService;
    }

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        costCol.setCellValueFactory(new PropertyValueFactory<>("cost"));
        sellPriceCol.setCellValueFactory(new PropertyValueFactory<>("sellPrice"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        minStockCol.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        supplierCol.setCellValueFactory(new PropertyValueFactory<>("supplier"));

        setupActionColumn();

        productsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.getQuantity() <= item.getMinStock()) {
                    // Light Red for Low Stock
                    setStyle("-fx-background-color: #fee2e2;");
                } else {
                    setStyle("");
                }
            }
        });

        refreshTable();
        updateDataSignature();
        setupAutoRefresh();
    }

    @FXML
    private void handleSearch() {
        String term = barcodeSearchField.getText();
        if (term == null || term.isBlank()) {
            showAlert(t("products.alert.search.empty"));
            return;
        }
        var results = productService.searchByBarcodeOrName(term.trim());
        if (results.isEmpty()) {
            showAlert(t("products.alert.search.none"));
            return;
        }
        productsTable.setItems(FXCollections.observableArrayList(results));
        productsTable.refresh();
    }

    @FXML
    private void handleAddNew() {
        openProductDialog(null);
    }

    @FXML
    private void handleShowAll() {
        barcodeSearchField.clear();
        refreshTable();
        productsTable.refresh();
    }

    private void refreshTable() {
        productsTable.setItems(FXCollections.observableArrayList(productService.getAll()));
        updateDataSignature();
    }

    private void refreshFromCurrentFilter() {
        if (productsTable == null) {
            return;
        }
        String term = barcodeSearchField != null ? barcodeSearchField.getText() : null;
        if (term != null && !term.isBlank()) {
            var results = productService.searchByBarcodeOrName(term.trim());
            productsTable.setItems(FXCollections.observableArrayList(results));
        } else {
            refreshTable();
        }
        productsTable.refresh();
        updateDataSignature();
    }

    private void setupAutoRefresh() {
        if (productsTable == null) {
            return;
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(REFRESH_SECONDS_VISIBLE), e -> {
            if (shouldRefresh()) {
                refreshFromCurrentFilter();
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
        AutoRefreshUtil.bind(autoRefreshTimeline, productsTable,
                (double) REFRESH_SECONDS_VISIBLE / (double) REFRESH_SECONDS_HIDDEN);
    }

    private boolean shouldRefresh() {
        if (isUserEditing()) {
            return false;
        }
        return hasDataChanged();
    }

    private boolean isUserEditing() {
        return barcodeSearchField != null && barcodeSearchField.isFocused();
    }

    private boolean hasDataChanged() {
        LocalDateTime latest = productService.getLatestUpdateTime();
        long count = productService.getTotalCount();
        boolean changed = !Objects.equals(latest, lastUpdatedAt) || count != lastCount;
        if (changed) {
            lastUpdatedAt = latest;
            lastCount = count;
        }
        return changed;
    }

    private void updateDataSignature() {
        lastUpdatedAt = productService.getLatestUpdateTime();
        lastCount = productService.getTotalCount();
    }

    private void setupActionColumn() {
        Callback<TableColumn<Product, Void>, TableCell<Product, Void>> cellFactory = col -> new TableCell<>() {
            private final Button editBtn = new Button(t("products.action.edit"));
            private final Button delBtn = new Button(t("products.action.delete"));
            private final HBoxWrapper box = new HBoxWrapper(editBtn, delBtn);

            {
                editBtn.getStyleClass().add("button-primary");
                delBtn.getStyleClass().add("button-primary");

                editBtn.setOnAction(e -> {
                    Product product = getTableView().getItems().get(getIndex());
                    openProductDialog(product);
                });

                delBtn.setOnAction(e -> {
                    Product product = getTableView().getItems().get(getIndex());
                    productService.deleteById(product.getId());
                    refreshTable();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        };
        actionCol.setCellFactory(cellFactory);
    }

    @FXML
    private TableColumn<Product, String> supplierCol;

    // ...

    // ...

    private void openProductDialog(Product existing) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));

        TextField name = new TextField();
        TextField barcode = new TextField();

        ComboBox<String> category = new ComboBox<>();
        category.setEditable(true);
        category.setItems(FXCollections.observableArrayList(productService.getAllCategories()));

        TextField supplier = new TextField();
        TextField cost = new TextField();
        TextField sellPrice = new TextField();
        TextField qty = new TextField();
        TextField minStock = new TextField();

        if (existing != null) {
            name.setText(existing.getName());
            barcode.setText(existing.getBarcode());
            category.setValue(existing.getCategory());
            supplier.setText(existing.getSupplier());
            cost.setText(String.valueOf(existing.getCost()));
            sellPrice.setText(String.valueOf(existing.getSellPrice()));
            qty.setText(String.valueOf(existing.getQuantity()));
            minStock.setText(String.valueOf(existing.getMinStock()));
        }

        grid.addRow(0, new Label(t("products.field.name")), name);
        grid.addRow(1, new Label(t("products.field.barcode")), barcode);
        grid.addRow(2, new Label(t("products.field.category")), category);
        grid.addRow(3, new Label(t("products.field.supplier")), supplier);
        grid.addRow(4, new Label(t("products.field.cost")), cost);
        grid.addRow(5, new Label(t("products.field.sellPrice")), sellPrice);
        grid.addRow(6, new Label(t("products.field.qty")), qty);
        grid.addRow(7, new Label(t("products.field.minStock")), minStock);

        Button saveBtn = new Button(t("products.dialog.save"));
        saveBtn.getStyleClass().add("button-primary");
        Button cancelBtn = new Button(t("products.dialog.cancel"));
        cancelBtn.getStyleClass().add("button-secondary");

        javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(10, saveBtn, cancelBtn);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        grid.add(buttons, 1, 8);

        javafx.stage.Stage dialog = com.library.pos.util.DialogUtil.createDialog(
                existing == null ? t("products.dialog.add.title") : t("products.dialog.edit.title"),
                grid,
                productsTable.getScene().getWindow());

        cancelBtn.setOnAction(e -> dialog.close());

        saveBtn.setOnAction(e -> {
            try {
                String catVal = category.getValue();
                if (catVal == null)
                    catVal = category.getEditor().getText();

                Product p = new Product(
                        name.getText().trim(),
                        barcode.getText().trim(),
                        catVal != null ? catVal : "",
                        Double.parseDouble(cost.getText().trim()),
                        Double.parseDouble(sellPrice.getText().trim()),
                        Integer.parseInt(qty.getText().trim()),
                        Integer.parseInt(minStock.getText().trim()),
                        supplier.getText().trim());

                if (existing != null) {
                    p.setId(existing.getId());
                }

                // Check duplicate barcode
                if (existing == null || !existing.getBarcode().equals(p.getBarcode())) {
                    if (productService.findByBarcode(p.getBarcode()).isPresent()) {
                        showAlert(t("products.alert.barcode.exists"));
                        return;
                    }
                }

                productService.save(p);
                refreshTable();
                dialog.close();
            } catch (Exception ex) {
                showAlert(t("products.alert.invalid") + ": " + ex.getMessage());
            }
        });

        dialog.showAndWait();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(t("products.alert.info.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String t(String key) {
        ResourceBundle bundle = resources != null ? resources : ResourceBundle.getBundle("messages");
        return bundle.getString(key);
    }

    private static class HBoxWrapper extends javafx.scene.layout.HBox {
        HBoxWrapper(Button edit, Button delete) {
            super(8, edit, delete);
            setStyle("-fx-padding: 2 6 2 6;");
        }
    }
}
