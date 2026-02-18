import re
import os

file_path = r'c:\Users\user\Library-Dr-Khaled\src\main\java\com\library\pos\controller\ProductsController.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports
imports = """import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;"""

content = re.sub(r'import javafx\.scene\.control\.\*;[\s\n]+import javafx\.scene\.control\.cell\.PropertyValueFactory;', imports, content)

# Update initialize method
init_method = """    @FXML
    public void initialize() {
        productsTable.setEditable(true);

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setName(event.getNewValue());
            productService.save(p);
            refreshFromCurrentFilter();
        });

        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        barcodeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        barcodeCol.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            try {
                p.setBarcode(event.getNewValue());
                productService.save(p);
            } catch (Exception ex) {
                showAlert(t("products.alert.barcode.exists"));
            }
            refreshFromCurrentFilter();
        });

        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setCellFactory(TextFieldTableCell.forTableColumn());
        categoryCol.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setCategory(event.getNewValue());
            productService.save(p);
            refreshFromCurrentFilter();
        });

        costCol.setCellValueFactory(new PropertyValueFactory<>("cost"));
        costCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        costCol.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setCost(event.getNewValue());
            productService.save(p);
            refreshFromCurrentFilter();
        });

        sellPriceCol.setCellValueFactory(new PropertyValueFactory<>("sellPrice"));
        sellPriceCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        sellPriceCol.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setSellPrice(event.getNewValue());
            productService.save(p);
            refreshFromCurrentFilter();
        });

        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        qtyCol.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setQuantity(event.getNewValue());
            productService.save(p);
            refreshFromCurrentFilter();
        });

        minStockCol.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        minStockCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        minStockCol.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setMinStock(event.getNewValue());
            productService.save(p);
            refreshFromCurrentFilter();
        });

        supplierCol.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        supplierCol.setCellFactory(TextFieldTableCell.forTableColumn());
        supplierCol.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setSupplier(event.getNewValue());
            productService.save(p);
            refreshFromCurrentFilter();
        });"""

content = re.sub(r'    @FXML\s+public void initialize\(\) \{[\s\S]*?supplierCol\.setCellValueFactory\(new PropertyValueFactory<>\("supplier"\)\);', init_method, content)

# Update isUserEditing
content = re.sub(r'private boolean isUserEditing\(\) \{[\s\S]*?return barcodeSearchField != null && barcodeSearchField\.isFocused\(\);[\s\S]*?\}', 
                 'private boolean isUserEditing() {\n        return (barcodeSearchField != null && barcodeSearchField.isFocused()) || (productsTable != null && productsTable.getEditingCell() != null);\n    }', 
                 content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Successfully patched ProductsController.java")
