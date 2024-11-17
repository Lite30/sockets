import java.io.*;
import java.net.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChatServer {
    private static final int PORT = 5000;
    private static final HashSet<ClientHandler> clients = new HashSet<>();
    private static final AESEncryption encryption;

    static {
        try {
            encryption = new AESEncryption();
            String key = encryption.getKeyAsString();
            System.out.println("Server started with encryption key: " + key);
            System.out.println("This key will be sent to all connecting clients");
        } catch (Exception e) {
            System.err.println("Critical error: Could not initialize encryption");
            throw new RuntimeException(e);
        }
    }

    private static String calculateHash(String message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(message.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculating hash", e);
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Secure Chat Server running on port " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from: " + clientSocket.getInetAddress());
                ClientHandler client = new ClientHandler(clientSocket);
                clients.add(client);
                new Thread(client).start();
            }
        } catch (IOException e) {
            System.err.println("Server failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println(encryption.getKeyAsString());
                System.out.println("Sent encryption key to client");

                username = in.readLine();
                System.out.println("User '" + username + "' joined the chat");
                broadcastMessage("SERVER: " + username + " has joined the chat!");

                String messagePacket;
                while ((messagePacket = in.readLine()) != null) {
                    try {
                        String[] parts = messagePacket.split("\\|");
                        String encryptedMessage = parts[0];
                        String receivedHash = parts[1];

                        String decryptedMessage = encryption.decrypt(encryptedMessage);
                        
                        String calculatedHash = calculateHash(decryptedMessage);
                        if (!calculatedHash.equals(receivedHash)) {
                            System.err.println("Warning: Message integrity check failed for message from " + username);
                            continue;
                        }
                        
                        System.out.println("Received from " + username + ": " + decryptedMessage);
                        broadcastMessage(username + ": " + decryptedMessage);
                    } catch (Exception e) {
                        System.err.println("Error processing message from " + username);
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client " + username);
                e.printStackTrace();
            } finally {
                try {
                    clients.remove(this);
                    broadcastMessage("SERVER: " + username + " has left the chat.");
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client connection");
                    e.printStackTrace();
                }
            }
        }

        private void broadcastMessage(String message) {
            try {
                String encryptedMessage = encryption.encrypt(message);
                String messageHash = calculateHash(message);
                String messagePacket = encryptedMessage + "|" + messageHash;
                
                System.out.println("Broadcasting: " + message);
                
                for (ClientHandler client : clients) {
                    client.out.println(messagePacket);
                }
            } catch (Exception e) {
                System.err.println("Error broadcasting message: " + message);
                e.printStackTrace();
            }
        }
    }
}
