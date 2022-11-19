package no.fintlabs.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

@Service
public class TrustStoreService extends Store {

    public KeyStore createTrustStore(String ca) throws GeneralSecurityException, IOException {

        KeyStore keyStore = createEmptyStore("jks");

        X509Certificate caCertificate = loadCA(ca);

        keyStore.setCertificateEntry("aiven ca", caCertificate);


        return keyStore;
    }
}
