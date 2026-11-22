
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class AdminDashboard {

    public static void show(Stage stage, String username) {

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

        userTable.getColumns().addAll(usernameCol, roleCol, activeCol);
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

        VBox layout = new VBox(12,
                heading,
                welcome,
                new Label("Registered Employees"),
                userTable,
                new Label("Recent Network Activity"),
                logTable,
                refreshButton
        );

        layout.setPadding(new Insets(20));

        VBox.setVgrow(userTable, Priority.ALWAYS);
        VBox.setVgrow(logTable, Priority.ALWAYS);

        stage.setTitle("SecureLink - Admin Dashboard");
        stage.setScene(new Scene(layout, 900, 650));
        stage.show();
    }
}
