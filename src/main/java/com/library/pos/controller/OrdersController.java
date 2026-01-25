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

    private final SaleService saleService;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public OrdersController(SaleService saleService) {
        this.saleService = saleService;
    }

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        dateCol.setCellValueFactory(cell -> {
            if (cell.getValue().getTimestamp() != null) {
                return new SimpleStringProperty(cell.getValue().getTimestamp().format(dtf));
            }
            return new SimpleStringProperty("");
        });

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
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        // Default: This month
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now());

        handleFilter();
    }

    @FXML
    public void handleFilter() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from != null && to != null) {
            LocalDateTime start = from.atStartOfDay();
            LocalDateTime end = to.atTime(LocalTime.MAX);
            ordersTable.setItems(FXCollections.observableArrayList(saleService.findByRange(start, end)));
        }
    }

    @FXML
    public void loadAll() {
        ordersTable.setItems(FXCollections.observableArrayList(saleService.getAll()));
    }
}
