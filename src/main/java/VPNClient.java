import java.io.*;
import java.net.*;
import java.util.Scanner;

public class VPNClient {

    private static final int PORT = 8080;

    public static void main(String[] args) {

        String serverAddress = args.length > 0
                ? args[0] : "127.0.0.1";

        Scanner scanner = new Scanner(System.in);

        try {

            Socket socket = new Socket(serverAddress, PORT);

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter output = new PrintWriter(
                    socket.getOutputStream(), true);

            System.out.println("================================");
            System.out.println("       SECURELINK CLIENT        ");
            System.out.println("================================");

            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            // Send credentials to server
            output.println("LOGIN|" + username + "|" + password);

            String response = input.readLine();

            if (response == null || !response.startsWith("LOGIN_OK|")) {
                System.out.println("Login failed: " + response);
                socket.close();
                return;
            }

            System.out.println("\nLogin successful!");
            System.out.println("Welcome, " + username);

            // Background thread listens for incoming messages
            Thread listener = new Thread(() -> {

                try {

                    String message;

                    while ((message = input.readLine()) != null) {

                        if (message.startsWith("FROM|")) {

                            String[] parts = message.split("\\|", 3);

                            try {
                                String decryptedMessage =
                                        EncryptionManager.decrypt(parts[2]);

                                System.out.println("\n\nNew message from "
                                        + parts[1] + ": " + decryptedMessage);

                            } catch (Exception e) {
                                System.out.println("\nUnable to decrypt message: "
                                        + e.getMessage());
                            }

                            System.out.print("Enter command: ");
                        }

                        else if (message.startsWith("USERS|")) {

                            String users = message.substring(6);

                            System.out.println("\nOnline employees: "
                                    + (users.isEmpty() ? "None" : users));

                            System.out.print("Enter command: ");
                        }

                        else if (message.startsWith("SENT|")) {

                            System.out.println("Message sent successfully!");
                            System.out.print("Enter command: ");
                        }

                        else if (message.startsWith("ERROR|")) {

                            System.out.println("Error: "
                                    + message.substring(6));

                            System.out.print("Enter command: ");
                        }
                    }

                } catch (IOException e) {
                    System.out.println("\nDisconnected from server.");
                }

            });

            listener.setDaemon(true);
            listener.start();

            System.out.println("\nCommands:");
            System.out.println("@username message - Send message");
            System.out.println("/users - View online employees");
            System.out.println("/quit - Disconnect");

            while (true) {

                System.out.print("\nEnter command: ");

                String command = scanner.nextLine();

                if (command.equalsIgnoreCase("/quit")) {
                    output.println("QUIT");
                    break;
                }

                else if (command.equalsIgnoreCase("/users")) {
                    output.println("USERS");
                }

                else if (command.startsWith("@")) {

                    int space = command.indexOf(' ');

                    if (space > 1 && space < command.length() - 1) {

                        String recipient = command.substring(1, space);
                        String text = command.substring(space + 1);

                        try {

                            String encryptedMessage =
                                    EncryptionManager.encrypt(text);

                            output.println(
                                    "MSG|" + recipient + "|" + encryptedMessage
                            );

                            System.out.println(
                                    "Message encrypted and transmitted.");

                        } catch (Exception e) {
                            System.out.println(
                                    "Encryption error: " + e.getMessage());
                        }

                    } else {
                        System.out.println("Format: @username Your message");
                    }

                } else {
                    System.out.println("Invalid command.");
                }
            }

            socket.close();
            System.out.println("Disconnected successfully.");

        } catch (IOException e) {
            System.out.println("Unable to connect to server: "
                    + e.getMessage());
        }
    }
}
