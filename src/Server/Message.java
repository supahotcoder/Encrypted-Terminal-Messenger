package Server;

import java.security.SecureRandom;

public class Message {
    private String from;
    private String to;
    private byte[] message;

    private byte[] key;

    public Message(String from, String to, String message) {
        this.from = from;
        this.to = to;
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            key = new byte[32];
            random.nextBytes(key);

            this.message = EncryptDecrypt.encrypt(message.getBytes(), key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getTo() {
        return to;
    }

    public String prepareRead() {
        try {
            String decrypted = new String(EncryptDecrypt.decrypt(message, key));
            return "FROM " + from + ": " + decrypted;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}