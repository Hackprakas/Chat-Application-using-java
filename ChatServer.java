import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    public static Set<String> userNames = new HashSet<>();
    private static Set<ClientHandler> clientHandlers = new HashSet<>();

    public static void main(String[] args) {
        int port = 1234;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user connected");

                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to broadcast message to all clients
    public static void broadcastMessage(String message, ClientHandler excludeUser) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler != excludeUser) {
                clientHandler.sendMessage(message);
            }
        }
    }

    // Method to add a new username
    public static boolean addUserName(String userName) {
        return userNames.add(userName);
    }

    // Method to remove a user
    public static void removeUser(String userName, ClientHandler clientHandler) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            clientHandlers.remove(clientHandler);
            System.out.println(userName + " has left the chat");
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter writer;
    private String userName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);

            printUsers();

            String userName = reader.readLine();
            this.userName = userName;
            if (ChatServer.addUserName(userName)) {
                writer.println("Welcome to the chat, " + userName + "!");
                ChatServer.broadcastMessage(userName + " has joined the chat.", this);

                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {
                    String messageToSend = "[" + userName + "]: " + clientMessage;
                    System.out.println(messageToSend);
                    ChatServer.broadcastMessage(messageToSend, this);
                }
            } else {
                writer.println("Username is already taken.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ChatServer.removeUser(userName, this);
            ChatServer.broadcastMessage(userName + " has left the chat.", this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Send message to the client
    public void sendMessage(String message) {
        writer.println(message);
    }

    // Print all currently logged-in users
    private void printUsers() {
        if (ChatServer.addUserName(userName)) {
            writer.println("Currently logged in users: " + ChatServer.userNames);
        } else {
            writer.println("No other users logged in.");
        }
    }
}
