package no.fintlabs.aiven

import no.fintlabs.operator.KafkaUserAndAcl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class AivenServiceSpec extends Specification {

    private MockWebServer mockWebServer
    private AivenProperties aivenProperties

    def setup() {
        mockWebServer = new MockWebServer()
        mockWebServer.start()

        aivenProperties = new AivenProperties()
        aivenProperties.setBaseUrl(mockWebServer.url("/").toString())
        aivenProperties.setProject("test_project")
        aivenProperties.setService("test_service")
    }

    def cleanup() {
        mockWebServer.shutdown()
    }

    def "init fetches CA certificate from expected endpoint"() {
        given:
        mockWebServer.enqueue(jsonResponse('{ "certificate": "ca-cert" }'))
        def service = new AivenService(webClient(), aivenProperties)

        when:
        service.init()

        then:
        service.getCa() == "ca-cert"
        with(takeRequest()) {
            method == "GET"
            path == "/project/test_project/kms/ca"
        }
    }

    def "createUserForService posts username and returns created user"() {
        given:
        mockWebServer.enqueue(jsonResponse('{ "message": "created", "user": { "username": "test-user", "password": "secret", "access_cert": "cert", "access_key": "key", "type": "service" } }'))
        def service = new AivenService(webClient(), aivenProperties)

        when:
        def createdUser = service.createUserForService("test-user")

        then:
        createdUser.username == "test-user"
        createdUser.password == "secret"
        with(takeRequest()) {
            method == "POST"
            path == "/project/test_project/service/test_service/user"
            body.readUtf8() == '{"username":"test-user"}'
        }
    }

    def "deleteUserForService issues delete to expected endpoint"() {
        given:
        mockWebServer.enqueue(new MockResponse().setResponseCode(204))
        def service = new AivenService(webClient(), aivenProperties)

        when:
        service.deleteUserForService("test-user")

        then:
        with(takeRequest()) {
            method == "DELETE"
            path == "/project/test_project/service/test_service/user/test-user"
        }
    }

    def "createAclEntryForTopic posts ACL request and returns matching ACL"() {
        given:
        mockWebServer.enqueue(jsonResponse('{ "acl": [ { "id": "other", "permission": "read", "topic": "other-topic", "username": "other-user" }, { "id": "acl-1", "permission": "readwrite", "topic": "topic-a", "username": "test-user" } ], "success": true }'))
        def service = new AivenService(webClient(), aivenProperties)
        def aclEntry = KafkaAclEntry.builder()
                .username("test-user")
                .topic("topic-a")
                .permission("readwrite")
                .build()

        when:
        def createdAcl = service.createAclEntryForTopic(aclEntry)

        then:
        createdAcl.id == "acl-1"
        createdAcl.topic == "topic-a"
        createdAcl.username == "test-user"
        with(takeRequest()) {
            method == "POST"
            path == "/project/test_project/service/test_service/acl"
            body.readUtf8() == '{"permission":"readwrite","topic":"topic-a","username":"test-user"}'
        }
    }

    def "createAclEntryForTopic rejects unsupported permission before calling Aiven"() {
        given:
        def service = new AivenService(webClient(), aivenProperties)
        def aclEntry = KafkaAclEntry.builder()
                .username("test-user")
                .topic("topic-a")
                .permission("consume")
                .build()

        when:
        service.createAclEntryForTopic(aclEntry)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "consume is not a valid Kafka ACL permission"
        mockWebServer.requestCount == 0
    }

    def "deleteAclEntryForService issues delete to expected endpoint"() {
        given:
        mockWebServer.enqueue(new MockResponse().setResponseCode(204))
        def service = new AivenService(webClient(), aivenProperties)

        when:
        service.deleteAclEntryForService("acl-123")

        then:
        with(takeRequest()) {
            method == "DELETE"
            path == "/project/test_project/service/test_service/acl/acl-123"
        }
    }

    def "updateAclEntries deletes removed ACLs creates missing ACLs and refetches current state"() {
        given:
        def expected = KafkaUserAndAcl.builder()
                .user(AivenServiceUser.fromUsername("test-user"))
                .aclEntries([
                        KafkaAclEntry.builder().id("keep").username("test-user").topic("topic-a").permission("read").build(),
                        KafkaAclEntry.builder().id("new-id").username("test-user").topic("topic-b").permission("write").build()
                ])
                .build()
        def actual = KafkaUserAndAcl.builder()
                .user(AivenServiceUser.fromUsername("test-user"))
                .aclEntries([
                        KafkaAclEntry.builder().id("remove-id").username("test-user").topic("topic-old").permission("read").build(),
                        KafkaAclEntry.builder().id("keep").username("test-user").topic("topic-a").permission("read").build()
                ])
                .build()
        def desired = KafkaUserAndAcl.builder()
                .user(AivenServiceUser.fromUsername("test-user"))
                .aclEntries([
                        KafkaAclEntry.builder().username("test-user").topic("topic-a").permission("read").build(),
                        KafkaAclEntry.builder().username("test-user").topic("topic-b").permission("write").build()
                ])
                .build()
        def service = new TrackingAivenService(aivenProperties)
        service.result = Optional.of(expected)

        when:
        def updated = service.updateAclEntries(actual, desired)

        then:
        service.deletedAclIds == ["remove-id"]
        service.createdAclEntries*.username == ["test-user"]
        service.createdAclEntries*.topic == ["topic-b"]
        service.createdAclEntries*.permission == ["write"]
        service.requestedUsers == ["test-user"]
        updated == expected
    }

    private WebClient webClient() {
        WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build()
    }

    private static MockResponse jsonResponse(String body) {
        new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(body)
    }

    private RecordedRequest takeRequest() {
        mockWebServer.takeRequest(1, TimeUnit.SECONDS)
    }

    private static class TrackingAivenService extends AivenService {
        List<String> deletedAclIds = []
        List<KafkaAclEntry> createdAclEntries = []
        List<String> requestedUsers = []
        Optional<KafkaUserAndAcl> result = Optional.empty()

        TrackingAivenService(AivenProperties properties) {
            super(WebClient.builder().baseUrl("http://localhost").build(), properties)
        }

        @Override
        void deleteAclEntryForService(String aclId) {
            deletedAclIds << aclId
        }

        @Override
        KafkaAclEntry createAclEntryForTopic(KafkaAclEntry aclEntry) {
            createdAclEntries << aclEntry
            KafkaAclEntry.builder()
                    .id("created-${createdAclEntries.size()}")
                    .username(aclEntry.username)
                    .topic(aclEntry.topic)
                    .permission(aclEntry.permission)
                    .build()
        }

        @Override
        Optional<KafkaUserAndAcl> getUserAndAcl(String username) {
            requestedUsers << username
            result
        }
    }
}
