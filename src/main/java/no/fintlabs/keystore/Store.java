package no.fintlabs.keystore;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Slf4j
public abstract class Store {

    protected X509Certificate loadCA(String ca) throws GeneralSecurityException {

        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(ca.getBytes()));
    }

    public String storeToBase64(KeyStore keyStore, char[] password) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            keyStore.store(out, password);
            return new String(Base64.getEncoder().encode(out.toByteArray()));
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            log.error("An error occurred when trying to store key store: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected KeyStore createEmptyStore(String type) throws IOException, GeneralSecurityException {

        KeyStore keyStore = KeyStore.getInstance(type);

        keyStore.load(null, null);

        return keyStore;

    }
}
