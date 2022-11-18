package no.fintlabs.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

@Service
public class KeyStoreService {

    private KeyStore createEmptyKeyStore() throws IOException, GeneralSecurityException {

        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        keyStore.load(null, null);

        return keyStore;

    }

    private X509Certificate loadCertificate(String accessCert) throws GeneralSecurityException {

        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(accessCert.getBytes()));

    }

    private X509Certificate loadCA(String ca) throws GeneralSecurityException {

        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(ca.getBytes()));

    }

    private PrivateKey loadPrivateKey(String accessKey) throws GeneralSecurityException {

        Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
        String encoded = parse.matcher(accessKey).replaceFirst("$1");

        byte[] keyDecoded = Base64.getMimeDecoder().decode(encoded);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyDecoded);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(keySpec);
    }

    public KeyStore createKeyStore(String cert, String key, String ca, char[] password) throws GeneralSecurityException, IOException {

        KeyStore keyStore = createEmptyKeyStore();
        X509Certificate publicCert = loadCertificate(cert);
        PrivateKey privateKey = loadPrivateKey(key);
        X509Certificate caCertificate = loadCA(ca);


        keyStore.setKeyEntry("1", privateKey, password, new Certificate[]{publicCert, caCertificate});


        return keyStore;
    }

    public String keyStoreToBase64(KeyStore keyStore, char[] password) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        keyStore.store(out, password);
        return new String(Base64.getEncoder().encode(out.toByteArray()));
    }
}
