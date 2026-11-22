import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class VPNServer {

    private static final int PORT = 8080;

    private static final ConcurrentMap<String, ClientHandler> clients =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {

        System.out.println("==================================");
        System.out.println("       SECURELINK VPN SERVER      ");
        System.out.println("==================================");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("Server started successfully!");
            System.out.println("Listening on port: " + PORT);
            System.out.println("Waiting for employees...\n");

            while (true) {
                Socket socket = serverSocket.accept();

                System.out.println("New connection from: "
                        + socket.getInetAddress());

                new Thread(new ClientHandler(socket)).start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    public static void broadcastUsers() {

        String users = clients.keySet()
                .stream()
                .sorted()
                .collect(Collectors.joining(","));

        for (ClientHandler client : clients.values()) {
            client.sendMessage("USERS|" + users);
        }
    }

    static class ClientHandler implements Runnable {

        private final Socket socket;
        private BufferedReader input;
        private PrintWriter output;
        private String username;
        private String role;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String message) {
            if (output != null) {
                output.println(message);
            }
        }

        @Override
        public void run() {

            try {
                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                output = new PrintWriter(
                        socket.getOutputStream(), true);

                // Receive login credentials
                String login = input.readLine();

                if (login == null) {
                    sendMessage("ERROR|No login request received");
                    return;
                }

                String[] credentials = login.split("\\|", 3);

                if (credentials.length != 3 ||
                        !credentials[0].equals("LOGIN")) {
                    sendMessage("ERROR|Invalid login request");
                    return;
                }

                username = credentials[1].trim();
                String password = credentials[2];

                if (!username.matches("[a-zA-Z0-9_]{3,20}")) {
                    sendMessage("ERROR|Invalid username");
                    username = null;
                    return;
                }

                // Authenticate using MySQL
                role = DatabaseManager.authenticateUser(username, password);

                if (role == null) {
                    sendMessage("ERROR|Invalid username or password");
                    username = null;
                    return;
                }

                // Prevent duplicate online usernames
                if (clients.putIfAbsent(username, this) != null) {
                    sendMessage("ERROR|Username already online");
                    username = null;
                    return;
                }

                sendMessage("LOGIN_OK|" + username);
                DatabaseManager.recordConnection(
                        username,
                        socket.getInetAddress().getHostAddress()
                );

                System.out.println("\n========== LOGIN LOG ==========");
                System.out.println("User: " + username);
                System.out.println("Role: " + role);
                System.out.println("Status: Authenticated");
                System.out.println("================================");

                broadcastUsers();

                String message;

                while ((message = input.readLine()) != null) {

                    String[] parts = message.split("\\|", 3);

                    if (parts[0].equals("USERS")) {

                        sendMessage("USERS|" +
                                String.join(",", clients.keySet()));
                    }

                    else if (parts[0].equals("MSG") && parts.length == 3) {

                        String recipient = parts[1];
                        String encryptedText = parts[2];

                        ClientHandler target = clients.get(recipient);

                        if (target != null) {

                            target.sendMessage(
                                    "FROM|" + username + "|" + encryptedText);

                            sendMessage("SENT|" + recipient);
                            DatabaseManager.logMessage(
                                    username,
                                    recipient,
                                    encryptedText
                            );

                            System.out.println("\n========== NETWORK LOG ==========");
                            System.out.println("Sender: " + username);
                            System.out.println("Receiver: " + recipient);
                            System.out.println("Encryption: AES-GCM");
                            System.out.println("Encrypted Payload: " + encryptedText);
                            System.out.println("Status: Forwarded");
                            System.out.println("=================================");

                        } else {
                            sendMessage("ERROR|Employee is offline");
                        }
                    }

                    else if (parts[0].equals("QUIT")) {
                        break;
                    }
                }

            } catch (IOException e) {
                System.out.println("Connection error: " + e.getMessage());

            } finally {

                if (username != null && clients.remove(username, this)) {

                    DatabaseManager.closeConnection(username);

                    System.out.println(username + " disconnected.");
                    broadcastUsers();
                }
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
