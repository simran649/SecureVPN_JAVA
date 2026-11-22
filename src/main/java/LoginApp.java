
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoginApp extends Application {

    @Override
    public void start(Stage stage) {

        Label heading = new Label("SecureLink");
        heading.setStyle("-fx-font-size: 27px; -fx-font-weight: bold;");

        Label subtitle = new Label("Secure Office Communication");
        subtitle.setStyle("-fx-text-fill: #666666;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle(
                "-fx-background-color: #263b5e;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;"
        );

        Label messageLabel = new Label();

        VBox layout = new VBox(15,
                heading,
                subtitle,
                usernameField,
                passwordField,
                loginButton,
                messageLabel
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(35));
        layout.setMaxWidth(330);
        layout.setStyle("-fx-background-color: white;");

        loginButton.setOnAction(event -> {

            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Please enter both fields.");
                return;
            }

            String role = DatabaseManager.authenticateUser(
                    username, password
            );

            if (role == null) {
                messageLabel.setText("Invalid username or password.");
                return;
            }

            messageLabel.setText("Login successful!");

            Label welcome = new Label("Welcome, " + username);
            welcome.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

            Label roleLabel = new Label("Role: " + role);

            VBox dashboard = new VBox(15, welcome, roleLabel);

            dashboard.setAlignment(Pos.CENTER);
            dashboard.setPadding(new Insets(30));
            dashboard.setStyle("-fx-background-color: white;");

            if (role.equals("ADMIN")) {
                AdminDashboard.show(stage, username, password);
            } else {
                EmployeeDashboard.show(stage, username, password);
            }
        });

        Scene scene = new Scene(layout, 450, 400);

        stage.setTitle("SecureLink Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
