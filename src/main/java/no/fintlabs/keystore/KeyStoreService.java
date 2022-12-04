package no.fintlabs.keystore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

@Slf4j
@Service
public class KeyStoreService extends Store {

    private X509Certificate loadCertificate(String accessCert) throws GeneralSecurityException {

        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(accessCert.getBytes()));

    }

    private PrivateKey loadPrivateKey(String accessKey) throws GeneralSecurityException {

        Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
        String encoded = parse.matcher(accessKey).replaceFirst("$1");

        byte[] keyDecoded = Base64.getMimeDecoder().decode(encoded);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyDecoded);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(keySpec);
    }

    public KeyStore createKeyStore(String cert, String key, String ca, char[] password) {

        try {
            KeyStore keyStore = createEmptyStore("PKCS12");
            X509Certificate publicCert = loadCertificate(cert);
            PrivateKey privateKey = loadPrivateKey(key);
            X509Certificate caCertificate = loadCA(ca);


            keyStore.setKeyEntry("1", privateKey, password, new Certificate[]{publicCert, caCertificate});


            return keyStore;
        } catch (IOException | GeneralSecurityException e) {
            log.error("An error occurred when creating key store: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String createKeyStoreAndGetAsBase64(String cert, String key, String ca, char[] password) {
        return storeToBase64(createKeyStore(cert, key, ca, password), password);
    }
}