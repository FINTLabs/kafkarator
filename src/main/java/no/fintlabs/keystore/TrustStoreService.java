package no.fintlabs.keystore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Slf4j
@Service
public class TrustStoreService extends Store {

    public KeyStore createTrustStore(String ca) {

        try {
            KeyStore keyStore = createEmptyStore("jks");
            X509Certificate caCertificate = loadCA(ca);

            keyStore.setCertificateEntry("aiven ca", caCertificate);

            return keyStore;
        } catch (IOException | GeneralSecurityException e) {
            log.error("An error occurred when creating trust store: {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public String verifyTrustStore(String base64KeyStore, String password) {
        try {
            byte[] decode = Base64.getDecoder().decode(base64KeyStore);
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new ByteArrayInputStream(decode), password.toCharArray());

            log.debug("Trust store is ok!");

            return base64KeyStore;

        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            log.debug("Unable to open truest store with error '{}'.", e.getMessage());
            log.error("We need to create a new one!");
            return null;
        }
    }

    public String createTrustStoreAndGetAsBase64(String ca, char[] password) {
        return storeToBase64(createTrustStore(ca), password);
    }
}
