package com.library.pos.controller;

import com.library.pos.model.Sale;
import com.library.pos.service.SaleService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class OrdersController {

    @FXML
    private TableView<Sale> ordersTable;
    @FXML
    private TableColumn<Sale, Long> idCol;
    @FXML
    private TableColumn<Sale, String> timeCol;
    @FXML
    private TableColumn<Sale, String> itemCol;
    @FXML
    private TableColumn<Sale, Integer> quantityCol;
    @FXML
    private TableColumn<Sale, String> notesCol;

    @FXML
    private TableColumn<Sale, String> dateCol;
    @FXML
    private TableColumn<Sale, String> customerCol;
    @FXML
    private TableColumn<Sale, String> workerCol;
    @FXML
    private TableColumn<Sale, Double> amountCol;
    @FXML
    private TableColumn<Sale, String> statusCol;

    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private javafx.scene.control.TextField filterProductField;
    @FXML
    private javafx.scene.control.ComboBox<com.library.pos.model.User> filterWorkerCombo;

    private final SaleService saleService;
    private final com.library.pos.service.UserService userService;
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm a");

    public OrdersController(SaleService saleService, com.library.pos.service.UserService userService) {
        this.saleService = saleService;
        this.userService = userService;
    }

    @FXML
    public void initialize() {
        // ID Column
        if (idCol != null) {
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        }

        // Setup filter combo
        filterWorkerCombo.setConverter(new javafx.util.StringConverter<com.library.pos.model.User>() {
            @Override
            public String toString(com.library.pos.model.User user) {
                return user == null ? "الكل" : user.getFullName();
            }

            @Override
            public com.library.pos.model.User fromString(String string) {
                return null; // Not needed
            }
        });
        filterWorkerCombo.setItems(FXCollections.observableArrayList());
        filterWorkerCombo.getItems().add(null); // Option for "All"
        filterWorkerCombo.getItems().addAll(userService.getAllWorkers());
        filterWorkerCombo.getSelectionModel().selectFirst();

        // Date & Time
        dateCol.setCellValueFactory(cell -> {
            if (cell.getValue().getTimestamp() != null) {
                return new SimpleStringProperty(cell.getValue().getTimestamp().format(dateFormat));
            }
            return new SimpleStringProperty("");
        });

        timeCol.setCellValueFactory(cell -> {
            if (cell.getValue().getTimestamp() != null) {
                return new SimpleStringProperty(cell.getValue().getTimestamp().format(timeFormat));
            }
            return new SimpleStringProperty("");
        });

        // Item & Qty
        itemCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

        customerCol.setCellValueFactory(cell -> {
            if (cell.getValue().getCustomer() != null) {
                return new SimpleStringProperty(cell.getValue().getCustomer().getCustomerName());
            }
            return new SimpleStringProperty("عميل نقدي");
        });

        workerCol.setCellValueFactory(cell -> {
            if (cell.getValue().getWorker() != null) {
                return new SimpleStringProperty(cell.getValue().getWorker().getFullName());
            }
            return new SimpleStringProperty("-");
        });

        amountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusCol.setCellValueFactory(cell -> {
            if (cell.getValue().getStatus() != null) {
                // Translate status if needed
                String s = cell.getValue().getStatus().name();
                if ("SOLD".equals(s))
                    return new SimpleStringProperty("مباع");
                if ("RETURNED".equals(s))
                    return new SimpleStringProperty("مرتجع");
                if ("DEFERRED".equals(s))
                    return new SimpleStringProperty("آجل");
                return new SimpleStringProperty(s);
            }
            return new SimpleStringProperty("-");
        });

        // Default: This month
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now());

        handleFilter();
    }

    @FXML
    public void handleFilter() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        // Get filters
        String productName = filterProductField != null ? filterProductField.getText() : null;
        if (productName != null && productName.trim().isEmpty())
            productName = null;

        com.library.pos.model.User worker = filterWorkerCombo.getValue();
        Long workerId = worker != null ? worker.getId() : null;

        if (from != null && to != null) {
            LocalDateTime start = from.atStartOfDay();
            LocalDateTime end = to.atTime(LocalTime.MAX);
            ordersTable.setItems(FXCollections.observableArrayList(
                    saleService.search(start, end, workerId, productName)));
        }
    }

    @FXML
    public void loadAll() {
        ordersTable.setItems(FXCollections.observableArrayList(saleService.getAll()));
    }
}
