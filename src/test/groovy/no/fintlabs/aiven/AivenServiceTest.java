package no.fintlabs.aiven;

import no.fintlabs.operator.KafkaUserAndAcl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AivenServiceTest {

    private MockWebServer mockWebServer;
    private AivenService service;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        AivenProperties aivenProperties = new AivenProperties();
        aivenProperties.setProject("test_project");
        aivenProperties.setService("test_service");

        service = new AivenService(webClient, aivenProperties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getUserAndAcl_ShouldReturnOptionalEmpty_WhenUserNotFound() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.NOT_FOUND.value())
                .setBody("User not found")
        );

        Optional<KafkaUserAndAcl> result = service.getUserAndAcl("user_not_found");

        assertEquals(Optional.empty(), result);
    }

    @Test
    void getUserAndAcl_ShouldThrowException_WhenServerError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setBody("Internal Server Error"));

        assertThrows(WebClientResponseException.class, () -> service.getUserAndAcl("user_with_server_error"));
    }

    @Test
    void getUserAndAcl_ShouldReturnUserAndAcl_WhenUserExists() {
        String userResponseJson = """
        {
            "user": {
                "username": "test_user"
            }
        }
        """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(userResponseJson)
                .addHeader("Content-Type", "application/json"));

        String aclResponseJson = """
                {
                    "acl": [
                        {
                          "id": "acl-1",
                          "permission": "read",
                          "username": "test_user",
                          "topic": "test-topic"
                        },
                        {
                          "id": "acl-2",
                          "permission": "write",
                          "username": "other_user",
                          "topic": "other-topic"
                        }
                    ]
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(aclResponseJson)
                .addHeader("Content-Type", "application/json"));

        Optional<KafkaUserAndAcl> result = service.getUserAndAcl("test_user");

        assertTrue(result.isPresent(), "Expected to find KafkaUserAndAcl");
        assertEquals("test_user", result.get().getUser().getUsername());
        assertEquals(1, result.get().getAclEntries().size());
        assertEquals("test_user", result.get().getAclEntries().get(0).getUsername());
        assertEquals("test-topic", result.get().getAclEntries().get(0).getTopic());
        assertEquals("read", result.get().getAclEntries().get(0).getPermission());
    }

    @Test
    void ensureUserForService_ShouldReturnExistingUser_WhenUserExists() throws InterruptedException {
        String userResponseJson = """
            {
              "user": {
                "username": "test_user"
              }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(userResponseJson)
                .addHeader("Content-Type", "application/json"));

        String aclResponseJson = """
            {
              "acl": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(aclResponseJson)
                .addHeader("Content-Type", "application/json"));

        AivenServiceUser result = service.ensureUserForService("test_user");

        assertEquals("test_user", result.getUsername());
        assertEquals(2, mockWebServer.getRequestCount());

        assertEquals("GET", mockWebServer.takeRequest().getMethod());
        assertEquals("GET", mockWebServer.takeRequest().getMethod());
    }

    @Test
    void ensureUserForService_ShouldCreateUser_WhenUserDoesNotExist() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.NOT_FOUND.value())
                .setBody("User not found"));

        String createUserResponseJson = """
            {
              "message": "created",
              "user": {
                "username": "test_user"
              }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(createUserResponseJson)
                .addHeader("Content-Type", "application/json"));

        AivenServiceUser result = service.ensureUserForService("test_user");

        assertEquals("test_user", result.getUsername());
        assertEquals(2, mockWebServer.getRequestCount());

        assertEquals("GET", mockWebServer.takeRequest().getMethod());
        assertEquals("POST", mockWebServer.takeRequest().getMethod());
    }
}
