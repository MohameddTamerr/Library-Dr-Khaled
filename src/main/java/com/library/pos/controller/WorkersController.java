package com.library.pos.controller;

import com.library.pos.model.User;
import com.library.pos.service.UserService;
import com.library.pos.util.AutoRefreshUtil;
import com.library.pos.util.DialogUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class WorkersController {

    @FXML
    private TableView<User> workersTable;
    @FXML
    private TableColumn<User, String> nameCol;
    @FXML
    private TableColumn<User, String> usernameCol;
    @FXML
    private TableColumn<User, String> phoneCol;
    @FXML
    private TableColumn<User, Double> rateCol;
    @FXML
    private TableColumn<User, Double> limitCol;
    @FXML
    private TableColumn<User, Double> currentWithdrawalCol;

    @FXML
    private TextField nameField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField passwordField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField rateField;
    @FXML
    private TextField limitField;

    @FXML
    private ComboBox<String> roleCombo;
    @FXML
    private javafx.scene.layout.VBox usernameBox;
    @FXML
    private javafx.scene.layout.VBox passwordBox;

    @FXML
    private Button addWorkerBtn;

    private Long editingUserId = null;

    private final UserService userService;
    private final com.library.pos.service.SaleService saleService;
    private Timeline autoRefreshTimeline;
    private static final int REFRESH_SECONDS_VISIBLE = 8;
    private static final int REFRESH_SECONDS_HIDDEN = 16;
    private LocalDateTime lastUserUpdatedAt;
    private long lastUserCount = -1;
    private Long lastAdvanceId = null;
    private long lastAdvanceCount = -1;

    @org.springframework.beans.factory.annotation.Autowired
    public WorkersController(UserService userService, com.library.pos.service.SaleService saleService) {
        this.userService = userService;
        this.saleService = saleService;
    }

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        rateCol.setCellValueFactory(new PropertyValueFactory<>("hourlyRate"));
        limitCol.setCellValueFactory(new PropertyValueFactory<>("salaryLimit"));
        currentWithdrawalCol.setCellValueFactory(new PropertyValueFactory<>("currentWithdrawal"));

        addActionButtonsToTable();
        setupRoleCombo();
        loadWorkers();
        updateDataSignature();
        setupAutoRefresh();
    }

    private void setupRoleCombo() {
        if (roleCombo == null) {
            return;
        }
        List<String> roles = List.of(
                getString("workers.role.worker"),
                getString("workers.role.delivery"));
        roleCombo.setItems(FXCollections.observableArrayList(roles));
        roleCombo.getSelectionModel().selectFirst();
        roleCombo.valueProperty().addListener((obs, old, value) -> updateRoleUI(value));
        updateRoleUI(roleCombo.getSelectionModel().getSelectedItem());
    }

    private void updateRoleUI(String selectedRole) {
        boolean isDelivery = isDeliveryRole(selectedRole);
        if (usernameBox != null) {
            usernameBox.setVisible(!isDelivery);
            usernameBox.setManaged(!isDelivery);
        }
        if (passwordBox != null) {
            passwordBox.setVisible(!isDelivery);
            passwordBox.setManaged(!isDelivery);
        }
    }

    private boolean isDeliveryRole(String selectedRole) {
        String deliveryLabel = getString("workers.role.delivery");
        return selectedRole != null && selectedRole.equals(deliveryLabel);
    }

    private com.library.pos.model.Role getSelectedRole() {
        if (roleCombo == null) {
            return com.library.pos.model.Role.WORKER;
        }
        return isDeliveryRole(roleCombo.getSelectionModel().getSelectedItem())
                ? com.library.pos.model.Role.DELIVERY_MEN
                : com.library.pos.model.Role.WORKER;
    }

    private String getString(String key) {
        try {
            return java.util.ResourceBundle.getBundle("messages").getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    private void addActionButtonsToTable() {
        TableColumn<User, Void> colBtn = new TableColumn<>("إجراءات");
        colBtn.setPrefWidth(120);

        javafx.util.Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new javafx.util.Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                final TableCell<User, Void> cell = new TableCell<>() {

                    private final Button btnEdit = new Button("تعديل");
                    private final Button btnDelete = new Button("حذف");
                    private final Button btnWithdraw = new Button("سلفة");
                    private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(5, btnWithdraw, btnEdit,
                            btnDelete);

                    {
                        btnEdit.setStyle(
                                "-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 5 10;");
                        btnDelete.setStyle(
                                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 5 10;");
                        btnWithdraw.setStyle(
                                "-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 5 10;");

                        btnWithdraw.setTooltip(new Tooltip("سحب مبلغ"));

                        pane.setAlignment(javafx.geometry.Pos.CENTER);

                        btnDelete.setOnAction((event) -> {
                            User data = getTableView().getItems().get(getIndex());
                            handleDeleteWorker(data);
                        });

                        btnEdit.setOnAction((event) -> {
                            User data = getTableView().getItems().get(getIndex());
                            handleEditWorker(data);
                        });

                        btnWithdraw.setOnAction((event) -> {
                            User data = getTableView().getItems().get(getIndex());
                            handleWithdraw(data);
                        });

                        // Add Report Button (Info icon)
                        Button btnReport = new Button("تقرير");
                        btnReport.setStyle(
                                "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 5 10;");
                        btnReport.setTooltip(new Tooltip("تقرير ساعات العمل"));
                        btnReport.setOnAction(e -> {
                            User data = getTableView().getItems().get(getIndex());
                            handleShowReport(data);
                        });

                        pane.getChildren().add(0, btnReport);
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                };
                return cell;
            }
        };

        colBtn.setCellFactory(cellFactory);
        workersTable.getColumns().add(colBtn);
    }

    private void handleWithdraw(User worker) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        if (workersTable != null && workersTable.getScene() != null) {
            dialog.initOwner(workersTable.getScene().getWindow());
        }

        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
        dialog.setTitle("سحب مبلغ");

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(16);
        root.setPadding(new javafx.geometry.Insets(24));
        root.setStyle(
                "-fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");

        Label header = new Label("سحب مبلغ للموظف: " + worker.getFullName());
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        double current = worker.getCurrentWithdrawal() != null ? worker.getCurrentWithdrawal() : 0.0;
        double limit = worker.getSalaryLimit() != null ? worker.getSalaryLimit() : 0.0;

        Label infoLabel = new Label(String.format("المسحوبات الحالية: %.2f / الحد الأقصى: %.2f", current, limit));
        infoLabel.setStyle("-fx-text-fill: #666;");

        TextField amountField = new TextField();
        amountField.setPromptText("أدخل المبلغ");

        TextArea reasonField = new TextArea();
        reasonField.setPromptText("سبب السلفة (اختياري)");
        reasonField.setPrefRowCount(2);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);

        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(10);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("إلغاء");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button("تسجيل");
        confirmBtn.setDefaultButton(true);
        confirmBtn.setOnAction(e -> {
            try {
                String text = amountField.getText();
                if (text == null || text.isBlank()) {
                    errorLabel.setText("الرجاء إدخال المبلغ");
                    errorLabel.setVisible(true);
                    return;
                }

                double amount = Double.parseDouble(text);
                if (amount <= 0) {
                    errorLabel.setText("يجب أن يكون المبلغ أكبر من صفر");
                    errorLabel.setVisible(true);
                    return;
                }

                if (current + amount > limit) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.initOwner(dialog); // Ensure alert is owned by dialog
                    alert.setTitle("تحذير");
                    alert.setHeaderText("تجاوز الحد المسموح!");
                    alert.setContentText("الموظف سحب " + current + " والحد هو " + limit + ".\nسيصبح الإجمالي: "
                            + (current + amount) + "\nهل تريد المتابعة؟");
                    if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                        return;
                    }
                }

                String reason = reasonField.getText();
                if (reason == null || reason.isBlank()) {
                    reason = "manual withdraw";
                }

                userService.addAdvance(worker, amount, reason);
                loadWorkers();
                dialog.close();
            } catch (NumberFormatException ex) {
                errorLabel.setText("الرقم غير صحيح");
                errorLabel.setVisible(true);
            } catch (Exception ex) {
                errorLabel.setText("خطأ: " + ex.getMessage());
                errorLabel.setVisible(true);
            }
        });

        btnBox.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().addAll(header, infoLabel, amountField, reasonField, errorLabel, btnBox);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 350);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();
    }

    private void loadWorkers() {
        workersTable.setItems(FXCollections.observableArrayList(userService.getStaff()));
        updateDataSignature();
    }

    private void setupAutoRefresh() {
        if (workersTable == null) {
            return;
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(REFRESH_SECONDS_VISIBLE), e -> {
            if (shouldRefresh()) {
                loadWorkers();
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
        AutoRefreshUtil.bind(autoRefreshTimeline, workersTable,
                (double) REFRESH_SECONDS_VISIBLE / (double) REFRESH_SECONDS_HIDDEN);
    }

    private boolean shouldRefresh() {
        if (isUserEditing()) {
            return false;
        }
        return hasDataChanged();
    }

    private boolean isUserEditing() {
        return (nameField != null && nameField.isFocused())
                || (usernameField != null && usernameField.isFocused())
                || (passwordField != null && passwordField.isFocused())
                || (phoneField != null && phoneField.isFocused())
                || (rateField != null && rateField.isFocused())
                || (limitField != null && limitField.isFocused())
                || (roleCombo != null && roleCombo.isFocused());
    }

    private boolean hasDataChanged() {
        LocalDateTime latestUserUpdate = userService.getStaffLatestUpdateTime();
        long userCount = userService.getStaffCount();
        Long latestAdvanceId = userService.getLatestAdvanceId();
        long advanceCount = userService.getAdvanceCount();

        boolean changed = !Objects.equals(latestUserUpdate, lastUserUpdatedAt)
                || userCount != lastUserCount
                || !Objects.equals(latestAdvanceId, lastAdvanceId)
                || advanceCount != lastAdvanceCount;

        if (changed) {
            lastUserUpdatedAt = latestUserUpdate;
            lastUserCount = userCount;
            lastAdvanceId = latestAdvanceId;
            lastAdvanceCount = advanceCount;
        }
        return changed;
    }

    private void updateDataSignature() {
        lastUserUpdatedAt = userService.getStaffLatestUpdateTime();
        lastUserCount = userService.getStaffCount();
        lastAdvanceId = userService.getLatestAdvanceId();
        lastAdvanceCount = userService.getAdvanceCount();
    }

    @FXML
    private void handleAddWorker() {
        try {
            String name = nameField.getText() != null ? nameField.getText().trim() : "";
            String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
            String password = passwordField.getText() != null ? passwordField.getText().trim() : "";
            String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
            Double rate = Double.parseDouble(rateField.getText());
            Double limit = Double.parseDouble(limitField.getText());
            com.library.pos.model.Role selectedRole = getSelectedRole();

            if (name == null || name.isBlank()) {
                showAlert("Error", "Please fill all required fields");
                return;
            }

            boolean isDelivery = selectedRole == com.library.pos.model.Role.DELIVERY_MEN;
            if (!isDelivery && (username == null || username.isBlank() || password == null || password.isBlank())) {
                showAlert("Error", "Please fill all required fields");
                return;
            }

            User workerToSave;
            if (editingUserId != null) {
                // Update existing
                workerToSave = userService.getStaff().stream()
                        .filter(u -> u.getId().equals(editingUserId))
                        .findFirst()
                        .orElse(new User());
                workerToSave.setFullName(name);
                if (!isDelivery) {
                    workerToSave.setUsername(username);
                    workerToSave.setPassword(password); // In real app, check if changed
                }
                workerToSave.setPhoneNumber(phone);
                workerToSave.setHourlyRate(rate);
                workerToSave.setSalaryLimit(limit);
                workerToSave.setRole(selectedRole);
            } else {
                // Create new
                if (isDelivery) {
                    username = userService.generateDeliveryUsername(name, phone);
                    password = userService.generateDeliveryPassword();
                }
                workerToSave = new User(username, password, selectedRole, name, rate, phone, limit);
            }

            userService.saveStaff(workerToSave);

            clearForm();
            loadWorkers();
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid number format for Rate or Limit");
        } catch (Exception e) {
            showAlert("Error", "Could not save worker: " + e.getMessage());
        }
    }

    private void clearForm() {
        nameField.clear();
        usernameField.clear();
        passwordField.clear();
        phoneField.clear();
        rateField.clear();
        limitField.clear();
        editingUserId = null;
        if (roleCombo != null) {
            roleCombo.getSelectionModel().selectFirst();
            updateRoleUI(roleCombo.getSelectionModel().getSelectedItem());
        }
        if (addWorkerBtn != null)
            addWorkerBtn.setText("إضافة موظف");
    }

    private void handleDeleteWorker(User worker) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        DialogUtil.initOwner(alert, workersTable.getScene() != null ? workersTable.getScene().getWindow() : null);
        alert.setTitle("تأكيد الحذف");
        alert.setHeaderText("حذف الموظف: " + worker.getFullName());
        alert.setContentText("هل أنت متأكد؟ لا يمكن التراجع عن هذا الإجراء.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            userService.deleteUser(worker.getId());
            loadWorkers();
        }
    }

    private void handleEditWorker(User worker) {
        editingUserId = worker.getId();
        nameField.setText(worker.getFullName());
        usernameField.setText(worker.getUsername());
        passwordField.setText(worker.getPassword());
        phoneField.setText(worker.getPhoneNumber());
        rateField.setText(String.valueOf(worker.getHourlyRate()));
        limitField.setText(String.valueOf(worker.getSalaryLimit()));
        if (roleCombo != null) {
            String roleValue = worker.getRole() == com.library.pos.model.Role.DELIVERY_MEN
                    ? getString("workers.role.delivery")
                    : getString("workers.role.worker");
            roleCombo.getSelectionModel().select(roleValue);
            updateRoleUI(roleValue);
        }

        if (addWorkerBtn != null)
            addWorkerBtn.setText("تحديث البيانات");
    }

    private void handleShowReport(User worker) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        if (workersTable != null && workersTable.getScene() != null) {
            dialog.initOwner(workersTable.getScene().getWindow());
        }
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setTitle("تقرير الموظف: " + worker.getFullName());

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(15);
        root.setPadding(new javafx.geometry.Insets(20));
        root.setStyle("-fx-background-color: white;");

        // Filter Section
        javafx.scene.layout.HBox filterBox = new javafx.scene.layout.HBox(10);
        filterBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        DatePicker startDate = new DatePicker(java.time.LocalDate.now().withDayOfMonth(1));
        DatePicker endDate = new DatePicker(java.time.LocalDate.now());
        Button showBtn = new Button("عرض");
        showBtn.setDefaultButton(true);

        filterBox.getChildren().addAll(new Label("من:"), startDate, new Label("إلى:"), endDate, showBtn);

        // Quick Select Buttons
        javafx.scene.layout.HBox quickBox = new javafx.scene.layout.HBox(10);
        quickBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Button todayBtn = new Button("اليوم");
        Button weekBtn = new Button("هذا الأسبوع");
        Button monthBtn = new Button("هذا الشهر");

        todayBtn.setOnAction(e -> {
            startDate.setValue(java.time.LocalDate.now());
            endDate.setValue(java.time.LocalDate.now());
            showBtn.fire();
        });

        weekBtn.setOnAction(e -> {
            startDate.setValue(java.time.LocalDate.now().minusDays(6));
            endDate.setValue(java.time.LocalDate.now());
            showBtn.fire();
        });

        monthBtn.setOnAction(e -> {
            startDate.setValue(java.time.LocalDate.now().withDayOfMonth(1));
            endDate.setValue(java.time.LocalDate.now().withDayOfMonth(java.time.LocalDate.now().lengthOfMonth()));
            showBtn.fire();
        });

        quickBox.getChildren().addAll(todayBtn, weekBtn, monthBtn);

        // Stats Area
        TextArea statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setPrefRowCount(10);
        statsArea.setStyle("-fx-font-family: monospace; -fx-font-size: 14px;");

        showBtn.setOnAction(e -> {
            java.time.LocalDate start = startDate.getValue();
            java.time.LocalDate end = endDate.getValue();
            if (start == null || end == null)
                return;

            // 1. Work Duration
            long minutes = userService.getWorkMinutes(worker, start, end);
            long hours = minutes / 60;
            long mins = minutes % 60;

            // 2. Sales Data
            java.time.LocalDateTime startDt = start.atStartOfDay();
            java.time.LocalDateTime endDt = end.atTime(java.time.LocalTime.MAX);

            // Fetch sales
            List<com.library.pos.model.Sale> sales = saleService.search(startDt, endDt, worker.getId(), null);

            double totalSales = 0;
            int salesCount = 0;
            double totalReturns = 0;
            int returnsCount = 0;

            for (com.library.pos.model.Sale s : sales) {
                if (s.getStatus() == com.library.pos.model.SaleStatus.RETURNED) {
                    totalReturns += Math.abs(s.getTotalAmount());
                    returnsCount++;
                } else {
                    totalSales += s.getTotalAmount();
                    salesCount++;
                }
            }

            // 3. Withdrawals
            Double withdrawals = userService.getWithdrawals(worker, start, end);
            if (withdrawals == null)
                withdrawals = 0.0;

            StringBuilder sb = new StringBuilder();
            sb.append("تقرير الفترة: ").append(start).append(" إلى ").append(end).append("\n");
            sb.append("--------------------------------------------------\n");
            sb.append(String.format("مدة العمل المسجلة:   %d ساعة و %d دقيقة\n", hours, mins));
            sb.append("--------------------------------------------------\n");
            sb.append(String.format("عدد المبيعات:        %d\n", salesCount));
            sb.append(String.format("إجمالي المبيعات:     %.2f\n", totalSales));
            sb.append("--------------------------------------------------\n");
            sb.append(String.format("عدد المرتجعات:       %d\n", returnsCount));
            sb.append(String.format("إجمالي المرتجعات:    %.2f\n", totalReturns));
            sb.append("--------------------------------------------------\n");
            sb.append(String.format("صافي المبيعات:       %.2f\n", totalSales - totalReturns));
            sb.append("--------------------------------------------------\n");
            sb.append(String.format("المسحوبات (سلف):     %.2f\n", withdrawals));
            sb.append("--------------------------------------------------\n");

            statsArea.setText(sb.toString());
        });

        // Trigger initial load
        showBtn.fire();

        root.getChildren().addAll(filterBox, quickBox, statsArea);

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 500, 500);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        DialogUtil.initOwner(alert, workersTable.getScene() != null ? workersTable.getScene().getWindow() : null);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}
