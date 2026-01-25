package com.library.pos.controller;

import com.library.pos.model.Sale;
import com.library.pos.model.SaleStatus;
import com.library.pos.service.SaleService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

@Component
public class DashboardController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private StackPane contentArea;
    @FXML
    private Button workersButton;

    private final ConfigurableApplicationContext applicationContext;
    private final SaleService saleService;

    @FXML
    private TabPane rangeTabs;
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private ComboBox<String> fromTimeField;
    @FXML
    private ComboBox<String> toTimeField;
    @FXML
    private TableView<Sale> salesTable;
    @FXML
    private TableColumn<Sale, String> dateCol;
    @FXML
    private TableColumn<Sale, String> timeCol;
    @FXML
    private TableColumn<Sale, String> itemCol;
    @FXML
    private TableColumn<Sale, String> qtyCol;
    @FXML
    private TableColumn<Sale, String> totalCol;
    @FXML
    private TableColumn<Sale, String> statusCol;
    @FXML
    private TableColumn<Sale, String> workerCol;
    @FXML
    private TableColumn<Sale, String> notesCol;

    private Parent defaultDashboardView;

    public DashboardController(ConfigurableApplicationContext applicationContext, SaleService saleService) {
        this.applicationContext = applicationContext;
        this.saleService = saleService;
    }

    @FXML
    public void initialize() {
        if (contentArea != null && !contentArea.getChildren().isEmpty()) {
            defaultDashboardView = (Parent) contentArea.getChildren().get(0);
        }

        if (salesTable != null) {
            configureSalesTable();
            setupTimeDropdowns();
            setupDefaultRange();
            hookTabChanges();
            loadSalesFromInputs();
        }
    }

    public void setUsername(String username) {
        welcomeLabel.setText(ResourceBundle.getBundle("messages").getString("dash.welcome").replace("{0}", username));
    }

    @FXML
    public void showDashboard() {
        if (defaultDashboardView != null) {
            contentArea.getChildren().setAll(defaultDashboardView);
            setupDefaultRange();
            loadSalesFromInputs();
        }
    }

    @FXML
    public void showWorkers() {
        loadView("/fxml/workers.fxml");
    }

    @FXML
    public void showProducts() {
        loadView("/fxml/products.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            loader.setResources(ResourceBundle.getBundle("messages"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void applyFilters() {
        loadSalesFromInputs();
    }

    private void configureSalesTable() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getTimestamp().toLocalDate().toString()));
        timeCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getTimestamp().toLocalTime().format(timeFormatter)));
        itemCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemName()));
        qtyCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getQuantity())));
        totalCol.setCellValueFactory(cell -> {
            double total = cell.getValue().getTotalAmount();
            if (cell.getValue().getStatus() == SaleStatus.RETURNED) {
                total = -Math.abs(total);
            }
            return new SimpleStringProperty(String.format(Locale.US, "%.2f", total));
        });
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        statusCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase(SaleStatus.RETURNED.name())) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    }
                }
            }
        });
        workerCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getWorker() != null ? cell.getValue().getWorker().getFullName() : ""));
        notesCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getNotes() != null ? cell.getValue().getNotes() : ""));
    }

    private void hookTabChanges() {
        rangeTabs.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            switch (newVal.intValue()) {
                case 0 -> setDayRange(LocalDate.now());
                case 1 -> setWeekRange(LocalDate.now());
                case 2 -> setMonthRange(LocalDate.now());
                default -> setDayRange(LocalDate.now());
            }
            loadSalesFromInputs();
        });
    }

    private void setupDefaultRange() {
        if (rangeTabs != null) {
            rangeTabs.getSelectionModel().select(0);
        }
        setDayRange(LocalDate.now());
    }

    private void setDayRange(LocalDate date) {
        fromDatePicker.setValue(date);
        toDatePicker.setValue(date);
        fromTimeField.setValue("12:00 AM");
        toTimeField.setValue("11:59 PM");
    }

    private void setWeekRange(LocalDate date) {
        DayOfWeek firstDay = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
        LocalDate start = date.with(TemporalAdjusters.previousOrSame(firstDay));
        LocalDate end = start.plusDays(6);
        fromDatePicker.setValue(start);
        toDatePicker.setValue(end);
        fromTimeField.setValue("12:00 AM");
        toTimeField.setValue("11:59 PM");
    }

    private void setMonthRange(LocalDate date) {
        LocalDate start = date.withDayOfMonth(1);
        LocalDate end = date.with(TemporalAdjusters.lastDayOfMonth());
        fromDatePicker.setValue(start);
        toDatePicker.setValue(end);
        fromTimeField.setValue("12:00 AM");
        toTimeField.setValue("11:59 PM");
    }

    private void loadSalesFromInputs() {
        LocalDate fromDate = fromDatePicker.getValue() != null ? fromDatePicker.getValue() : LocalDate.now();
        LocalDate toDate = toDatePicker.getValue() != null ? toDatePicker.getValue() : fromDate;

        LocalTime fromTime = parseTimeOrDefault(fromTimeField.getValue(), LocalTime.MIN);
        LocalTime toTime = parseTimeOrDefault(toTimeField.getValue(), LocalTime.of(23, 59));

        LocalDateTime start = LocalDateTime.of(fromDate, fromTime);
        LocalDateTime end = LocalDateTime.of(toDate, toTime);

        if (end.isBefore(start)) {
            LocalDateTime temp = start;
            start = end;
            end = temp;
        }

        salesTable.setItems(FXCollections.observableArrayList(saleService.findByRange(start, end)));
    }

    private LocalTime parseTimeOrDefault(String raw, LocalTime fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            DateTimeFormatter parser = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
            return LocalTime.parse(raw, parser);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void setupTimeDropdowns() {
        if (fromTimeField == null || toTimeField == null) {
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
        var items = FXCollections.<String>observableArrayList();
        for (int hour = 0; hour < 24; hour++) {
            items.add(LocalTime.of(hour, 0).format(formatter));
        }
        toTimeField.setItems(items);
        fromTimeField.setItems(items);
    }
}
