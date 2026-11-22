import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class VPNServer {

    private static final int PORT = 8080;

    // Stores all currently connected employees
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

                System.out.println("New connection received from: "
                        + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);

                Thread thread = new Thread(handler);
                thread.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // Sends the current online user list to all clients
    public static void broadcastUsers() {

        String users = clients.keySet()
                .stream()
                .sorted()
                .collect(Collectors.joining(","));

        for (ClientHandler client : clients.values()) {
            client.sendMessage("USERS|" + users);
        }
    }

    // Handles an individual employee connection
    static class ClientHandler implements Runnable {

        private final Socket socket;
        private BufferedReader input;
        private PrintWriter output;
        private String username;

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

                // Receive login request
                String login = input.readLine();

                if (login == null || !login.startsWith("LOGIN|")) {
                    sendMessage("ERROR|Invalid login request");
                    return;
                }

                username = login.substring(6).trim();

                if (!username.matches("[a-zA-Z0-9_]{3,20}")) {
                    sendMessage("ERROR|Invalid username");
                    return;
                }

                // Register employee if username is not already online
                if (clients.putIfAbsent(username, this) != null) {
                    sendMessage("ERROR|Username already online");
                    return;
                }

                sendMessage("LOGIN_OK|" + username);

                System.out.println(username + " connected successfully.");

                broadcastUsers();

                String message;

                // Continuously listen for client messages
                while ((message = input.readLine()) != null) {

                    String[] parts = message.split("\\|", 3);

                    if (parts[0].equals("USERS")) {
                        sendMessage("USERS|" +
                                String.join(",", clients.keySet()));
                    }

                    else if (parts[0].equals("MSG") && parts.length == 3) {

                        String recipient = parts[1];
                        String text = parts[2];

                        ClientHandler target = clients.get(recipient);

                        if (target != null) {

                            target.sendMessage(
                                    "FROM|" + username + "|" + text);

                            sendMessage("SENT|" + recipient);

                            System.out.println(username + " -> "
                                    + recipient + ": " + text);

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
