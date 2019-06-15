package Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class UserDatabase {

    private List<User> users;
    private byte[] key;
    private List<byte[]> salts;
    private String fileName;

    public UserDatabase(String fileName) {
        this.users = new ArrayList<>();
        this.salts = new ArrayList<>();
        this.fileName = fileName;
        this.key = fileName.getBytes(Charset.defaultCharset());
        loadDatabaseFile(fileName);
    }

    private void loadDatabaseFile(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));

            String line;

            while ((line = reader.readLine()) != null) {
                decryptUser(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void decryptUser(String s) throws Exception {
        String[] passAndSalt = s.split(" ");

        byte[] encrypted = EncryptDecrypt.hexStringToByteArray(passAndSalt[0]);
        byte[] encSalt = EncryptDecrypt.hexStringToByteArray(passAndSalt[1]);

        byte[] salt = EncryptDecrypt.decrypt(encSalt, key);
        String decrypted = new String(EncryptDecrypt.decrypt(encrypted, key));

        String[] strings = decrypted.split(" ");
        User user = new User(strings[0], strings[1]);

        salts.add(salt);
        users.add(user);
    }

    private void addUserToFile(User user) {
        try {
            File file = new File(fileName);
            FileWriter fileWriter = new FileWriter(file, true);

            byte[] fileContent = EncryptDecrypt.encrypt(user.toString().getBytes(), key);
            byte[] salt = EncryptDecrypt.encrypt(salts.get(salts.size() - 1), key);

            fileWriter.write(EncryptDecrypt.byteArrayToHexString(fileContent));
            fileWriter.write(" ");
            fileWriter.write(EncryptDecrypt.byteArrayToHexString(salt));
            fileWriter.write("\n");
            fileWriter.flush();

        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    public void addUser(String username, String password) {
        if (findUser(username) != null) return;
        byte[] salt = new byte[64];
        SecureRandom random = null;
        try {
            random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            random.nextBytes(salt);

            salts.add(salt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        User user = new User(username, hashPassword(password,salt));

        users.add(user);
        addUserToFile(user);
    }

    public boolean authUser(String username, String password) {
        User user = findUser(username);
        byte[] salt = salts.get(users.indexOf(user));
        return (user != null) && checkPass(password, user.getPassword(), salt);
    }

    public boolean checkUser(String username) {
        return findUser(username) != null;
    }

    private User findUser(String username) {
        List<User> usr = users.stream()
                .filter(us -> us.getUsername().equals(username)).collect(Collectors.toList());
        return (usr.size() == 0) ? null : usr.get(0);
    }

    private String hashPassword(String plainTextPassword, byte[] salt) {
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);

            byte[] bytes = md.digest(plainTextPassword.getBytes());
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++){
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return generatedPassword;
    }

    private boolean checkPass(String plainPassword, String hashedPassword, byte[] salt) {
        return hashPassword(plainPassword,salt).equals(hashedPassword);
    }
}
