package no.fintlabs.operator

import no.fintlabs.aiven.KafkaAclEntry
import no.fintlabs.aiven.AivenServiceUser
import spock.lang.Specification

class AivenServiceUserAndAclModelSpec extends Specification {

    def "KafkaUser should be equal only by username"() {
        given:
        def user1 = AivenServiceUser.fromUsername("test")
        def user2 = AivenServiceUser
                .builder()
                .username("test")
                .password("topsecret")
                .access_cert("cert")
                .access_key("key")
                .type("normal")
                .build()

        when:
        def equals = user1 == user2

        then:
        equals
    }

    def "KafkaAclEntry should be equal even if id is not equal"() {
        def acl1 = KafkaAclEntry
                .builder()
                .username("test")
                .topic("test")
                .permission("read")
                .build()
        def acl2 = KafkaAclEntry
                .builder()
                .username("test")
                .topic("test")
                .permission("read")
                .id("1")
                .build()

        when:
        def equals = acl1 == acl2

        then:
        equals
    }

    def "Desired and actual KafkaUserAndAcl should be equal"() {
        given:
        def desired = KafkaUserAndAcl.builder()
                .user(AivenServiceUser.fromUsername("test"))
                .aclEntries(
                        Arrays.asList(
                                KafkaAclEntry
                                        .builder()
                                        .username("test")
                                        .topic("test")
                                        .permission("read")
                                        .build(),
                                KafkaAclEntry
                                        .builder()
                                        .username("test")
                                        .topic("test1")
                                        .permission("read")
                                        .build()
                        )
                )
                .build()

        def actual = KafkaUserAndAcl.builder()
                .user(
                        AivenServiceUser
                                .builder()
                                .username("test")
                                .password("topsecret")
                                .access_cert("cert")
                                .access_key("key")
                                .type("normal")
                                .build()
                )
                .aclEntries(
                        Arrays.asList(
                                KafkaAclEntry
                                        .builder()
                                        .username("test")
                                        .topic("test")
                                        .permission("read")
                                        .id("1")
                                        .build(),
                                KafkaAclEntry
                                        .builder()
                                        .username("test")
                                        .topic("test1")
                                        .permission("read")
                                        .id("2")
                                        .build()
                        )
                )
                .build()

        when:
        def equals = desired == actual

        then:
        equals
    }
}
