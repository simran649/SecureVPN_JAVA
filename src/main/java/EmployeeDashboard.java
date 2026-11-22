
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class EmployeeDashboard {

    private static final int PORT = 8080;

    public static void show(
            Stage stage, String username, String password) {

        try {
            String serverAddress = "127.0.0.1";

            Socket socket = new Socket(serverAddress, PORT);

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter output = new PrintWriter(
                    socket.getOutputStream(), true);

            output.println("LOGIN|" + username + "|" + password);

            String response = input.readLine();

            if (response == null || !response.startsWith("LOGIN_OK|")) {
                socket.close();

                new Alert(Alert.AlertType.ERROR,
                        "Server login failed: " + response).showAndWait();
                return;
            }

            // Header
            Label heading = new Label("SecureLink");
            heading.setStyle("-fx-font-size: 23px; -fx-font-weight: bold;");

            Label status = new Label("Connected");
            status.setStyle("-fx-text-fill: green;");

            HBox header = new HBox(15, heading, status);
            header.setPadding(new Insets(10));

            // Online employees
            Label usersHeading = new Label("Online Employees");

            ListView<String> userList = new ListView<>();
            userList.setPrefWidth(180);

            VBox sidebar = new VBox(10, usersHeading, userList);
            sidebar.setPadding(new Insets(10));
            sidebar.setPrefWidth(200);

            // Chat area
            Label chatHeading = new Label("Messages");

            TextArea chatArea = new TextArea();
            chatArea.setEditable(false);
            chatArea.setWrapText(true);

            TextField messageField = new TextField();
            messageField.setPromptText("Type your message...");

            Button sendButton = new Button("Send");

            HBox messageBox = new HBox(10, messageField, sendButton);
            HBox.setHgrow(messageField, Priority.ALWAYS);

            VBox chatPanel = new VBox(
                    10, chatHeading, chatArea, messageBox);

            chatPanel.setPadding(new Insets(10));
            VBox.setVgrow(chatArea, Priority.ALWAYS);
            HBox.setHgrow(chatPanel, Priority.ALWAYS);

            HBox mainContent = new HBox(10, sidebar, chatPanel);
            VBox.setVgrow(mainContent, Priority.ALWAYS);

            VBox root = new VBox(10, header, mainContent);
            root.setPadding(new Insets(10));

            Scene scene = new Scene(root, 800, 550);

            stage.setTitle("SecureLink - Employee");
            stage.setScene(scene);
            stage.show();

            // Send encrypted messages
            Runnable sendMessage = () -> {

                String recipient = userList.getSelectionModel()
                        .getSelectedItem();

                String message = messageField.getText().trim();

                if (recipient == null) {
                    chatArea.appendText(
                            "Select an employee first.\n");
                    return;
                }

                if (message.isEmpty()) {
                    return;
                }

                try {
                    String encrypted =
                            EncryptionManager.encrypt(message);

                    output.println(
                            "MSG|" + recipient + "|" + encrypted);

                    chatArea.appendText(
                            "You → " + recipient + ": " + message + "\n");

                    messageField.clear();

                } catch (Exception e) {
                    chatArea.appendText(
                            "Encryption error: " + e.getMessage() + "\n");
                }
            };

            sendButton.setOnAction(event -> sendMessage.run());

            messageField.setOnAction(event -> sendMessage.run());

            // Receive messages from server
            Thread listener = new Thread(() -> {

                try {
                    String incoming;

                    while ((incoming = input.readLine()) != null) {

                        String received = incoming;

                        Platform.runLater(() -> {

                            if (received.startsWith("USERS|")) {

                                String users = received.substring(6);

                                userList.getItems().clear();

                                if (!users.isEmpty()) {
                                    for (String user : users.split(",")) {
                                        if (!user.equals(username)) {
                                            userList.getItems().add(user);
                                        }
                                    }
                                }
                            }

                            else if (received.startsWith("FROM|")) {

                                String[] parts =
                                        received.split("\\|", 3);

                                if (parts.length == 3) {
                                    try {
                                        String decrypted =
                                                EncryptionManager.decrypt(parts[2]);

                                        chatArea.appendText(
                                                parts[1] + ": " + decrypted + "\n");

                                    } catch (Exception e) {
                                        chatArea.appendText(
                                                "Unable to decrypt message.\n");
                                    }
                                }
                            }

                            else if (received.startsWith("ERROR|")) {
                                chatArea.appendText(
                                        "Server error: "
                                                + received.substring(6) + "\n");
                            }
                        });
                    }

                } catch (IOException e) {
                    Platform.runLater(() ->
                            status.setText("Disconnected"));
                }

            });

            // Request the current online employee list
            output.println("USERS");

            stage.setOnCloseRequest(event -> {
                output.println("QUIT");

                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            });

        } catch (IOException e) {

            new Alert(Alert.AlertType.ERROR,
                    "Unable to connect to SecureLink server: "
                            + e.getMessage()).showAndWait();
        }
    }
}
