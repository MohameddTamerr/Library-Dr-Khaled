package com.library.pos.controller;

import com.library.pos.model.User;
import com.library.pos.service.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

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
    private TextField nameField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField passwordField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField rateField;
    @FXML
    private TextField limitField;

    private final UserService userService;

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

        loadWorkers();
    }

    private void loadWorkers() {
        workersTable.setItems(FXCollections.observableArrayList(userService.getAllWorkers()));
    }

    @FXML
    private void handleAddWorker() {
        try {
            String name = nameField.getText();
            String username = usernameField.getText();
            String password = passwordField.getText();
            String phone = phoneField.getText();
            String address = addressField.getText();
            Double rate = Double.parseDouble(rateField.getText());
            Double limit = Double.parseDouble(limitField.getText());

            User newWorker = new User(username, password, com.library.pos.model.Role.WORKER, name, rate, address, phone,
                    limit);
            userService.saveWorker(newWorker);

            clearForm();
            loadWorkers();
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format");
        }
    }

    private void clearForm() {
        nameField.clear();
        usernameField.clear();
        passwordField.clear();
        phoneField.clear();
        addressField.clear();
        rateField.clear();
        limitField.clear();
    }
}
