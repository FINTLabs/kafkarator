package no.fintlabs.operator

import io.javaoperatorsdk.operator.api.reconciler.Context
import no.fintlabs.aiven.AivenProperties
import no.fintlabs.aiven.AivenService
import no.fintlabs.aiven.AivenServiceUser
import no.fintlabs.aiven.KafkaAclEntry
import spock.lang.Specification

class KafkaUserAndAclDependentResourceSpec extends Specification {

    private AivenService aivenService = Mock()
    private AivenProperties aivenProperties = new AivenProperties(service: "kafka-service")
    private KafkaUserAndAclDependentResource resource =
            new KafkaUserAndAclDependentResource(new KafkaUserAndAclWorkflow(), aivenService, aivenProperties)
    private Context<KafkaUserAndAclCrd> context = Mock()

    def "desired derives username from metadata and maps all ACLs"() {
        given:
        def primary = primaryResource()

        when:
        def desired = resource.desired(primary, context)

        then:
        desired.user.username == "flais-io_platform_sample-user"
        desired.aclEntries*.topic == ["topic-a", "topic-b"]
        desired.aclEntries*.permission == ["read", "write"]
        desired.aclEntries*.username == ["flais-io_platform_sample-user", "flais-io_platform_sample-user"]
    }

    def "create provisions user and ACLs through Aiven service"() {
        given:
        def desired = KafkaUserAndAcl.builder()
                .user(AivenServiceUser.fromUsername("resolved-user"))
                .aclEntries([
                        KafkaAclEntry.builder().username("resolved-user").topic("topic-a").permission("read").build(),
                        KafkaAclEntry.builder().username("resolved-user").topic("topic-b").permission("write").build()
                ])
                .build()
        def createdUser = AivenServiceUser.builder()
                .username("resolved-user")
                .password("secret")
                .accessCert("cert")
                .accessKey("key")
                .type("service")
                .build()

        when:
        def created = resource.create(desired, primaryResource(), context)

        then:
        1 * aivenService.createUserForService("resolved-user") >> createdUser
        1 * aivenService.createAclEntryForTopic({
            it.topic == "topic-a" && it.permission == "read" && it.username == "resolved-user"
        }) >> KafkaAclEntry.builder().id("acl-1").username("resolved-user").topic("topic-a").permission("read").build()
        1 * aivenService.createAclEntryForTopic({
            it.topic == "topic-b" && it.permission == "write" && it.username == "resolved-user"
        }) >> KafkaAclEntry.builder().id("acl-2").username("resolved-user").topic("topic-b").permission("write").build()
        created.user == createdUser
        created.aclEntries*.id == ["acl-1", "acl-2"]
    }

    def "fetchResources returns singleton when Aiven has current resource"() {
        given:
        def current = KafkaUserAndAcl.builder()
                .user(AivenServiceUser.fromUsername("flais-io_platform_sample-user"))
                .build()
        aivenService.getUserAndAcl("flais-io_platform_sample-user") >> Optional.of(current)

        expect:
        resource.fetchResources(primaryResource()) == [current] as Set
    }

    def "fetchResources returns empty set when Aiven has no current resource"() {
        given:
        aivenService.getUserAndAcl("flais-io_platform_sample-user") >> Optional.empty()

        expect:
        resource.fetchResources(primaryResource()).isEmpty()
    }

    def "update delegates ACL diff handling to Aiven service"() {
        given:
        def actual = KafkaUserAndAcl.builder().user(AivenServiceUser.fromUsername("user")).build()
        def desired = KafkaUserAndAcl.builder().user(AivenServiceUser.fromUsername("user")).build()
        def updated = KafkaUserAndAcl.builder().user(AivenServiceUser.fromUsername("user")).build()

        when:
        def result = resource.update(actual, desired, primaryResource(), context)

        then:
        1 * aivenService.updateAclEntries(actual, desired) >> updated
        result == updated
    }

    def "delete removes all ACLs before deleting the user"() {
        given:
        def secondary = KafkaUserAndAcl.builder()
                .user(AivenServiceUser.fromUsername("resolved-user"))
                .aclEntries([
                        KafkaAclEntry.builder().id("acl-1").username("resolved-user").topic("topic-a").permission("read").build(),
                        KafkaAclEntry.builder().id("acl-2").username("resolved-user").topic("topic-b").permission("write").build()
                ])
                .build()
        context.getSecondaryResource(KafkaUserAndAcl) >> Optional.of(secondary)

        when:
        resource.delete(primaryResource(), context)

        then:
        1 * aivenService.deleteAclEntryForService("acl-1")
        1 * aivenService.deleteAclEntryForService("acl-2")
        1 * aivenService.deleteUserForService("resolved-user")
    }

    def "delete is a no-op when no secondary resource exists"() {
        given:
        context.getSecondaryResource(KafkaUserAndAcl) >> Optional.empty()

        when:
        resource.delete(primaryResource(), context)

        then:
        0 * aivenService._
    }

    private static KafkaUserAndAclCrd primaryResource() {
        def primary = new KafkaUserAndAclCrd()
        primary.metadata.name = "sample-user"
        primary.metadata.namespace = "default"
        primary.metadata.labels.put("fintlabs.no/team", "platform")
        primary.metadata.labels.put("fintlabs.no/org-id", "flais.io")
        primary.spec = KafkaUserAndAclSpec.builder()
                .acls([
                        acl("topic-a", "read"),
                        acl("topic-b", "write")
                ])
                .build()
        primary
    }

    private static KafkaUserAndAclSpec.Acl acl(String topic, String permission) {
        def acl = new KafkaUserAndAclSpec.Acl()
        acl.topic = topic
        acl.permission = permission
        acl
    }
}
