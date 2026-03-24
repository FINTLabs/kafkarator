package no.fintlabs.keystore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Enumeration;
import java.util.regex.Pattern;

@Slf4j
@Service
public class KeyStoreService extends Store {

    public record KeyStoreInspection(boolean readable, Instant notAfter, String errorMessage) {

        public static KeyStoreInspection valid(Instant notAfter) {
            return new KeyStoreInspection(true, notAfter, null);
        }

        public static KeyStoreInspection invalid(String errorMessage) {
            return new KeyStoreInspection(false, null, errorMessage);
        }

        public boolean needsRotation(Duration threshold) {
            if (!readable || notAfter == null) {
                return true;
            }

            Duration normalizedThreshold = threshold == null ? Duration.ZERO : threshold;
            Instant rotationDeadline = Instant.now().plus(normalizedThreshold);

            return !notAfter.isAfter(rotationDeadline);
        }
    }

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

    public String verifyKeyStore(String base64KeyStore, String password) {
        KeyStoreInspection inspection = inspectKeyStore(base64KeyStore, password);

        if (!inspection.readable()) {
            return null;
        }

        log.debug("Key store is ok!");
        return base64KeyStore;
    }

    public KeyStoreInspection inspectKeyStore(String base64KeyStore, String password) {
        try {
            byte[] decode = Base64.getDecoder().decode(base64KeyStore);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(decode), password.toCharArray());

            X509Certificate certificate = resolveLeafCertificate(keyStore);

            return KeyStoreInspection.valid(certificate.getNotAfter().toInstant());
        } catch (IllegalArgumentException | KeyStoreException | CertificateException | IOException |
                 NoSuchAlgorithmException e) {
            log.debug("Unable to open key store with error '{}'.", e.getMessage());
            return KeyStoreInspection.invalid(e.getMessage());
        }
    }

    public String createKeyStoreAndGetAsBase64(String cert, String key, String ca, char[] password) {
        return storeToBase64(createKeyStore(cert, key, ca, password), password);
    }

    private X509Certificate resolveLeafCertificate(KeyStore keyStore) throws KeyStoreException {
        Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();

            if (keyStore.isKeyEntry(alias)) {
                Certificate[] certificateChain = keyStore.getCertificateChain(alias);
                if (certificateChain != null && certificateChain.length > 0 && certificateChain[0] instanceof X509Certificate certificate) {
                    return certificate;
                }
            }

            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate instanceof X509Certificate x509Certificate) {
                return x509Certificate;
            }
        }

        throw new KeyStoreException("No X509 certificate found in key store");
    }
}
