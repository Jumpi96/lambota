package utils;

import java.util.Random;

public class HashGenerator {
    public static String generateShortHash() {
        return Integer.toString(new Random().nextInt(100000));
    }
}
