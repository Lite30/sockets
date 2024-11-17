import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;
    private static AESEncryption encryption;

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
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to secure chat server");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String encryptionKey = in.readLine();
            encryption = new AESEncryption(encryptionKey);
            System.out.println("Secure encryption initialized with server's key");

            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter your username: ");
            String username = scanner.nextLine();
            out.println(username);

            Thread receiverThread = new Thread(() -> {
                try {
                    String messagePacket;
                    while ((messagePacket = in.readLine()) != null) {
                        try {
                            String[] parts = messagePacket.split("\\|");
                            String encryptedMessage = parts[0];
                            String receivedHash = parts[1];

                            String decryptedMessage = encryption.decrypt(encryptedMessage);
                            
                            String calculatedHash = calculateHash(decryptedMessage);
                            if (!calculatedHash.equals(receivedHash)) {
                                System.err.println("Warning: Message integrity check failed!");
                                continue;
                            }
                            
                            System.out.println(decryptedMessage);
                        } catch (Exception e) {
                            System.err.println("Error decrypting message: " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server");
                }
            });
            receiverThread.start();

            String message;
            while (true) {
                message = scanner.nextLine();
                if (message.equalsIgnoreCase("/quit")) {
                    break;
                }
                try {
                    String messageHash = calculateHash(message);
                    String encryptedMessage = encryption.encrypt(message);
                    String messagePacket = encryptedMessage + "|" + messageHash;
                    out.println(messagePacket);
                } catch (Exception e) {
                    System.err.println("Error sending message: " + e.getMessage());
                }
            }

            socket.close();
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
