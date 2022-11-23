package no.fintlabs.keystore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

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

    public String createTrustStoreAndGetAsBase64(String ca, char[] password) {
        return storeToBase64(createTrustStore(ca), password);
    }
}
