package com.library.pos.controller;

import com.library.pos.model.WastedItem;
import com.library.pos.service.WastedItemService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WastedItemsController {

    @FXML private TableView<WastedItem> wastedTable;
    @FXML private TableColumn<WastedItem, String>  dateCol;
    @FXML private TableColumn<WastedItem, String>  productNameCol;
    @FXML private TableColumn<WastedItem, Integer> quantityCol;
    @FXML private TableColumn<WastedItem, Double>  costPerUnitCol;
    @FXML private TableColumn<WastedItem, Double>  totalCostCol;
    @FXML private TableColumn<WastedItem, String>  workerCol;
    @FXML private TableColumn<WastedItem, String>  notesCol;

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private TextField  searchField;
    @FXML private Label      totalLossLabel;

    private final WastedItemService wastedItemService;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm");

    public WastedItemsController(WastedItemService wastedItemService) {
        this.wastedItemService = wastedItemService;
    }

    @FXML
    public void initialize() {
        configureTable();
        fromDatePicker.setValue(LocalDate.now().minusDays(30));
        toDatePicker.setValue(LocalDate.now());
        loadAll();
    }

    private void configureTable() {
        dateCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getWastedAt() != null
                        ? d.getValue().getWastedAt().format(FMT) : ""));

        productNameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getProductName()));

        quantityCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getQuantity() != null
                        ? d.getValue().getQuantity() : 0).asObject());

        costPerUnitCol.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().getCostPerUnit() != null
                        ? d.getValue().getCostPerUnit() : 0.0).asObject());
        costPerUnitCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : String.format("%.2f ج.م", v));
            }
        });

        totalCostCol.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().getTotalCostLoss() != null
                        ? d.getValue().getTotalCostLoss() : 0.0).asObject());
        totalCostCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(String.format("%.2f ج.م", v));
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                }
            }
        });

        workerCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getWorker() != null
                        ? d.getValue().getWorker().getFullName() : "-"));

        notesCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getNotes() != null
                        ? d.getValue().getNotes() : ""));
    }

    private void loadAll() {
        List<WastedItem> items = wastedItemService.getAll();
        applyToTable(items);
    }

    @FXML
    private void handleFilter() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to   = toDatePicker.getValue();
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to   == null) to   = LocalDate.now();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(LocalTime.MAX);
        List<WastedItem> items = wastedItemService.getByRange(start, end);
        // also apply text filter
        String term = searchField.getText();
        if (term != null && !term.isBlank()) {
            String lower = term.trim().toLowerCase();
            items = items.stream()
                    .filter(w -> w.getProductName() != null && w.getProductName().toLowerCase().contains(lower))
                    .toList();
        }
        applyToTable(items);
        // update total loss label for the period
        double loss = wastedItemService.getTotalCostLoss(start, end);
        totalLossLabel.setText(String.format("%.2f ج.م", loss));
    }

    @FXML
    private void handleSearch() {
        handleFilter();
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        loadAll();
    }

    private void applyToTable(List<WastedItem> items) {
        wastedTable.setItems(FXCollections.observableArrayList(items));
        double total = items.stream()
                .mapToDouble(w -> w.getTotalCostLoss() != null ? w.getTotalCostLoss() : 0.0)
                .sum();
        totalLossLabel.setText(String.format("%.2f ج.م", total));
    }

    /** Called externally after a new wasted entry was saved to refresh the table. */
    public void refresh() {
        loadAll();
    }
}
