package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

public class HashGenerator {
    public static String generateShortHash() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            Random rd = new Random();
            byte[] randomBytes = new byte[8];
            rd.nextBytes(randomBytes);
            md.update(randomBytes);
            byte[] hashBytes = md.digest();
            String encoded = Base64.getEncoder().encodeToString(hashBytes);
            return encoded.substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error occurred while generating short hash: " + e.getMessage());
            return "";
        }
    }
}
