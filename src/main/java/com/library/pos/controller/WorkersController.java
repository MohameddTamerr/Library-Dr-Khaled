package com.library.pos.controller;

import com.library.pos.model.User;
import com.library.pos.service.UserService;
import com.library.pos.util.AutoRefreshUtil;
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
    private Timeline autoRefreshTimeline;
    private static final int REFRESH_SECONDS_VISIBLE = 8;
    private static final int REFRESH_SECONDS_HIDDEN = 16;
    private LocalDateTime lastUserUpdatedAt;
    private long lastUserCount = -1;
    private Long lastAdvanceId = null;
    private long lastAdvanceCount = -1;

    public WorkersController(UserService userService) {
        this.userService = userService;
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
            String name = nameField.getText();
            String username = usernameField.getText();
            String password = passwordField.getText();
            String phone = phoneField.getText();
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
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate start = now.withDayOfMonth(1);
        java.time.LocalDate end = now.withDayOfMonth(now.lengthOfMonth());

        long minutes = userService.getWorkMinutes(worker, start, end);
        long hours = minutes / 60;
        long mins = minutes % 60;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("تقرير العمل");
        alert.setHeaderText("تقرير شهر " + now.getMonth().toString());
        alert.setContentText("الموظف: " + worker.getFullName() + "\n" +
                "ساعات العمل هذا الشهر: " + hours + " ساعة و " + mins + " دقيقة.\n" +
                "المسحوبات: " + (worker.getCurrentWithdrawal() != null ? worker.getCurrentWithdrawal() : 0.0));
        alert.show();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}
