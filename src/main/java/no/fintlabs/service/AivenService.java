package no.fintlabs.service;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.model.*;
import no.fintlabs.operator.AivenKafkaUserAndAcl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Slf4j
@Component
public class AivenService {

    @Value("${aiven.base_url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final HttpHeaders headers;

    public AivenService(@Value("${aiven.token}") String token) {
        this.restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
    }

    public Set<AivenKafkaUserAndAcl> getUserAndAcl(String project, String serviceName, String username, List<String> topics) {
        String userUrl = baseUrl + "/project/" + project + "/service/" + serviceName + "/user/" + username;
        String aclUrl = baseUrl + "/project/" + project + "/service/" + serviceName + "/acl";
        CreateUserResponse userResponse;
        try {
            userResponse = restTemplate.exchange(userUrl, HttpMethod.GET, new HttpEntity<>(headers), CreateUserResponse.class).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return Collections.emptySet();
        }
        CreateAclEntryResponse aclResponse = restTemplate.exchange(aclUrl, HttpMethod.GET, new HttpEntity<>(headers), CreateAclEntryResponse.class).getBody();
        List<Acl> acl = new ArrayList<>();
        if (aclResponse != null && aclResponse.getAcl() != null) {
            for (String topic : topics) {
                acl.add(aclResponse.getAclByUsernameAndTopic(username, topic));
            }
        }
        if (userResponse != null && acl.size() > 0) {
            return Set.of(new AivenKafkaUserAndAcl(userResponse, acl));
        }
        return Collections.emptySet();
    }

    public CreateUserResponse createUserForService(String project, String serviceName, String username) {
        log.debug("Creating user {} for service {}", username, serviceName);
        String url = baseUrl + "/project/{project_name}/service/{service_name}/user";
        Map<String, String> params = new HashMap<>();
        params.put("project_name", project);
        params.put("service_name", serviceName);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);

        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(url, entity, CreateUserResponse.class, params);
    }

    public void deleteUserForService(String project, String serviceName, String username) {
        log.debug("Deleting user {} from service {}", username, serviceName);
        String url = baseUrl + "/project/{project_name}/service/{service_name}/user/{username}";
        HashMap<String, String> params = new HashMap<>();
        params.put("project_name", project);
        params.put("service_name", serviceName);
        params.put("username", username);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class, params);
    }

    public CreateAclEntryResponse createAclEntryForTopic(String project, String serviceName, String topic, String username, String permission) {
        log.debug("Creating ACL entry for topic {} for user {} with permission {}", topic, username, permission);
        String url = baseUrl + "/project/{project_name}/service/{service_name}/acl";
        Map<String, String> params = new HashMap<>();
        params.put("project_name", project);
        params.put("service_name", serviceName);

        CreateAclEntryRequest request = new CreateAclEntryRequest();
        CreateAclEntryResponse response = new CreateAclEntryResponse();

        String[] legalPermissions = {"admin", "read", "write", "readwrite"};
        if (Arrays.asList(legalPermissions).contains(permission.toLowerCase())) {
            request.setPermission(permission);
        } else {
            response.setMessage("Illegal permission, must be one of: " + Arrays.toString(legalPermissions));
            response.setSuccess(false);
            return response;
        }
        request.setUsername(username);
        request.setTopic(topic);

        HttpEntity<CreateAclEntryRequest> entity = new HttpEntity<>(request, headers);
        response = restTemplate.postForObject(url, entity, CreateAclEntryResponse.class, params);

        assert response != null;
        if (response.getMessage().equalsIgnoreCase("added")) {
            response.setSuccess(true);
        }
        return response;
    }

    public void deleteAclEntryForService(String project, String serviceName, String aclId) {
        log.debug("Deleting ACL entry for service {}", serviceName);
        String url = baseUrl + "/project/{project_name}/service/{service_name}/acl/{acl_id}";
        HashMap<String, String> params = new HashMap<>();
        params.put("project_name", project);
        params.put("service_name", serviceName);
        params.put("acl_id", aclId);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class, params);
    }

    public String getAclId(String projectName, String serviceName, String username, String topic) {
        String aclUrl = baseUrl + "/project/" + projectName + "/service/" + serviceName + "/acl";
        CreateAclEntryResponse aclResponse = restTemplate.exchange(aclUrl, HttpMethod.GET, new HttpEntity<>(headers), CreateAclEntryResponse.class).getBody();

        return aclResponse != null ? aclResponse.getAclByUsernameAndTopic(username, topic).getId() : null;
    }

    public String getCaCert(String projectName) {
        String url = baseUrl + "/project/" + projectName + "/kms/ca";
        CaCertResponse response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), CaCertResponse.class).getBody();
        return response != null ? response.getCertificate() : null;
    }

    public OutputStream createKeyStore(String accessCert, String accessKey) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            // TODO: change password
            char[] password = "password".toCharArray();

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Certificate certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(accessCert.getBytes()));
            // TODO: Cant create certificate from key
//            KeySpec keySpec = new PKCS8EncodedKeySpec(accessKey.getBytes());
//            Key accesskey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
            // Certificate key = certificateFactory.generateCertificate(new ByteArrayInputStream(accessKey.getBytes()));

            keyStore.load(null, password);
            keyStore.setCertificateEntry("cert", certificate);
            // TODO: Set Access Key entry
//            keyStore.setKeyEntry("key", accesskey, password, new Certificate[]{certificate});

            OutputStream outputStream = new ByteArrayOutputStream();
            keyStore.store(outputStream, password);
            return outputStream;

        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public OutputStream createTrustStore(String caCert) {
        /*
        keytool -import  \
            -file ca.pem \
            -alias CA    \
            -keystore client.truststore.jks
        */
        // TODO: Produce truststore.jks from CA cert

        return null;
    }
}
