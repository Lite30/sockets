import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SecureHashUtil {
    // Salt length in bytes
    private static final int SALT_LENGTH = 16;
    
    /**
     * Generates a random salt for hashing
     * @return byte array containing the salt
     */
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
    
    /**
     * Creates a SHA-256 hash of the password with the provided salt
     * @param password The password to hash
     * @param salt The salt to use
     * @return byte array containing the hashed password
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    public static byte[] hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
        // Create MessageDigest instance for SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        
        // Add salt to digest
        md.update(salt);
        
        // Add password bytes to digest
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        
        // Get the hashed password
        return md.digest(passwordBytes);
    }
    
    /**
     * Converts a byte array to a hexadecimal string
     * @param hash The byte array to convert
     * @return A string of hexadecimal characters
     */
    public static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Example usage of the secure hash functions
     */
    public static void main(String[] args) {
        try {
            String password = "MySecurePassword123";
            
            // Generate a random salt
            byte[] salt = generateSalt();
            
            // Hash the password with the salt
            byte[] hashedPassword = hashPassword(password, salt);
            
            // Convert to hex string for storage or display
            String hexHash = bytesToHex(hashedPassword);
            String hexSalt = bytesToHex(salt);
            
            System.out.println("Original Password: " + password);
            System.out.println("Salt (hex): " + hexSalt);
            System.out.println("Hashed Password (hex): " + hexHash);
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algorithm not available");
            e.printStackTrace();
        }
    }
}
