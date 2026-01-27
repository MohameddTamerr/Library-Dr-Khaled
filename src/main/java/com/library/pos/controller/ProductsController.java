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
import java.util.Optional;

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
            showAlert("Enter a name or barcode to search.");
            return;
        }
        var results = productService.searchByBarcodeOrName(term.trim());
        if (results.isEmpty()) {
            showAlert("No products found for this search.");
            return;
        }
        productsTable.setItems(FXCollections.observableArrayList(results));
        productsTable.refresh();
    }

    @FXML
    private void handleAddNew() {
        Optional<Product> result = showProductDialog(null);
        result.ifPresent(product -> {
            if (productService.findByBarcode(product.getBarcode()).isPresent()) {
                showAlert("Barcode already exists.");
                return;
            }
            productService.save(product);
            refreshTable();
        });
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
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBoxWrapper box = new HBoxWrapper(editBtn, delBtn);

            {
                editBtn.getStyleClass().add("button-primary");
                delBtn.getStyleClass().add("button-primary");

                editBtn.setOnAction(e -> {
                    Product product = getTableView().getItems().get(getIndex());
                    Optional<Product> updated = showProductDialog(product);
                    updated.ifPresent(p -> {
                        if (!product.getBarcode().equals(p.getBarcode())
                                && productService.findByBarcode(p.getBarcode()).isPresent()) {
                            showAlert("Barcode already exists.");
                            return;
                        }
                        product.setName(p.getName());
                        product.setBarcode(p.getBarcode());
                        product.setCategory(p.getCategory());
                        product.setCost(p.getCost());
                        product.setSellPrice(p.getSellPrice());
                        product.setQuantity(p.getQuantity());
                        product.setMinStock(p.getMinStock());
                        productService.save(product);
                        refreshTable();
                    });
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

    private Optional<Product> showProductDialog(Product existing) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Product" : "Edit Product");

        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

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

        grid.addRow(0, new Label("Name"), name);
        grid.addRow(1, new Label("Barcode"), barcode);
        grid.addRow(2, new Label("Category"), category);
        grid.addRow(3, new Label("Supplier"), supplier);
        grid.addRow(4, new Label("Cost"), cost);
        grid.addRow(5, new Label("Sell Price"), sellPrice);
        grid.addRow(6, new Label("Quantity"), qty);
        grid.addRow(7, new Label("Min Stock"), minStock);

        dialog.getDialogPane().setContent(grid);

        // Validation needed to prevent nulls

        dialog.setResultConverter(button -> {
            if (button == saveButton) {
                try {
                    String catVal = category.getValue();
                    if (catVal == null)
                        catVal = category.getEditor().getText();

                    return new Product(
                            name.getText().trim(),
                            barcode.getText().trim(),
                            catVal != null ? catVal : "",
                            Double.parseDouble(cost.getText().trim()),
                            Double.parseDouble(sellPrice.getText().trim()),
                            Integer.parseInt(qty.getText().trim()),
                            Integer.parseInt(minStock.getText().trim()),
                            supplier.getText().trim());
                } catch (Exception e) {
                    showAlert("Please enter valid values in all fields.");
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class HBoxWrapper extends javafx.scene.layout.HBox {
        HBoxWrapper(Button edit, Button delete) {
            super(8, edit, delete);
            setStyle("-fx-padding: 2 6 2 6;");
        }
    }
}
