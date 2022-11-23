package no.fintlabs.keystore;

import java.util.Base64;

public class TestUtil {

    public static boolean isBase64(String path) {
        try {
            Base64.getDecoder().decode(path);
            return true;
        } catch(IllegalArgumentException e) {
            return false;
        }
    }
}
