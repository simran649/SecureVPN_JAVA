import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class AdminDashboard {

    public static void show(Stage stage, String username, String password) {

        Label heading = new Label("SecureLink | Admin Dashboard");
        heading.setStyle("-fx-font-size: 23px; -fx-font-weight: bold;");

        Label welcome = new Label("Logged in as: " + username);

        // Employee table
        TableView<String[]> userTable = new TableView<>();

        TableColumn<String[], String> usernameCol =
                new TableColumn<>("Username");
        usernameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[0]));

        TableColumn<String[], String> roleCol =
                new TableColumn<>("Role");
        roleCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[1]));

        TableColumn<String[], String> activeCol =
                new TableColumn<>("Account Status");
        activeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[2]));

        TableColumn<String[], String> connectionCol =
                new TableColumn<>("Connection Status");

        connectionCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[3]));

        userTable.getColumns().addAll(
                usernameCol,
                roleCol,
                activeCol,
                connectionCol
        );
        userTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        for (String[] user : DatabaseManager.getAllUsers()) {
            userTable.getItems().add(user);
        }

        // Network activity table
        TableView<String[]> logTable = new TableView<>();

        String[] headings = {
                "Sender", "Receiver", "Encryption", "Status", "Time"
        };

        for (int i = 0; i < headings.length; i++) {
            final int index = i;

            TableColumn<String[], String> column =
                    new TableColumn<>(headings[i]);

            column.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue()[index]));

            logTable.getColumns().add(column);
        }

        logTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        for (String[] log : DatabaseManager.getRecentLogs()) {
            logTable.getItems().add(log);
        }

        Button refreshButton = new Button("Refresh Data");

        refreshButton.setOnAction(event -> {
            userTable.getItems().setAll(DatabaseManager.getAllUsers());
            logTable.getItems().setAll(DatabaseManager.getRecentLogs());
        });

        Button messagesButton = new Button("Open Messages");

        messagesButton.setOnAction(event -> {
            Stage chatStage = new Stage();
            EmployeeDashboard.show(chatStage, username, password);
        });

        HBox buttons = new HBox(10, refreshButton, messagesButton);

        VBox layout = new VBox(12,
                heading,
                welcome,
                new Label("Registered Employees"),
                userTable,
                new Label("Recent Network Activity"),
                logTable,
                buttons
        );

        layout.setPadding(new Insets(20));

        VBox.setVgrow(userTable, Priority.ALWAYS);
        VBox.setVgrow(logTable, Priority.ALWAYS);

        stage.setTitle("SecureLink - Admin Dashboard");
        stage.setScene(new Scene(layout, 900, 650));
        stage.show();
    }
}
