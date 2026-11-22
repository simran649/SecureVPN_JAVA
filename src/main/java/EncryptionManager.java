import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionManager {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    // Same salt is used by all clients in this project.
    // The salt is not a password or secret.
    private static final byte[] SALT =
            "SecureLink-Salt-2026".getBytes(StandardCharsets.UTF_8);

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final SecretKeySpec SECRET_KEY = createKey();

    private static SecretKeySpec createKey() {

        String password = System.getenv("SECURELINK_SECRET");

        if (password == null || password.length() < 16) {
            throw new IllegalStateException(
                    "SECURELINK_SECRET must be set and contain at least 16 characters."
            );
        }

        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    SALT,
                    120000,
                    256
            );

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] key = factory.generateSecret(spec).getEncoded();

            spec.clearPassword();

            return new SecretKeySpec(key, "AES");

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Key generation failed", e);
        }
    }

    // Encrypts a message and returns Base64-encoded ciphertext.
    public static String encrypt(String message) {

        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            RANDOM.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    SECRET_KEY,
                    new GCMParameterSpec(TAG_LENGTH, nonce)
            );

            byte[] encrypted = cipher.doFinal(
                    message.getBytes(StandardCharsets.UTF_8)
            );

            // Combine nonce and ciphertext before encoding.
            ByteBuffer buffer = ByteBuffer.allocate(
                    nonce.length + encrypted.length
            );

            buffer.put(nonce);
            buffer.put(encrypted);

            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    // Decrypts the received ciphertext.
    public static String decrypt(String encryptedMessage) {

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedMessage);

            if (combined.length < NONCE_LENGTH + 16) {
                throw new IllegalArgumentException("Invalid encrypted message");
            }

            ByteBuffer buffer = ByteBuffer.wrap(combined);

            byte[] nonce = new byte[NONCE_LENGTH];
            buffer.get(nonce);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    SECRET_KEY,
                    new GCMParameterSpec(TAG_LENGTH, nonce)
            );

            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(
                    "Decryption failed: message authentication failed", e
            );
        }
    }
}
