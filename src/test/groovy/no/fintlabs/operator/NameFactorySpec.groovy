package no.fintlabs.operator

import spock.lang.Specification

class NameFactorySpec extends Specification {

    def "Generated name should include orgId, team and UUID"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/team", "testteam")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "flais.io")
        crd.getMetadata().setName("fint-data-service")

        when:
        def name = NameFactory.nameFromMetadata(crd)

        then:
        def parts = name.split("-")
        parts.length >= 4

        and: "UUID Should me last and 16 characters"
        parts[-1] ==~ /^[a-f0-9]{16}$/

        and: "Final string should be max 64 characters"
        name.length() <= 64

        and: "OrgID, team, and name should be included"
        name.contains("test")
        name.contains("flais")
        name.contains("fint")
    }

    def "Should handle long orgId, team and name correctly"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/team", "very-long-team-name-that-blows-all-borders")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "insanely.long.organisation.identifier.that.exceeds.all.limits")
        crd.getMetadata().setName("well-ok-we-know-this-name-is-too-long-now-right")

        when:
        def name = NameFactory.nameFromMetadata(crd)

        then:
        name.length() <= 64

        and: def uuid = name.split("-")[-1]
        uuid.length() == 16
        uuid ==~ /^[a-f0-9]{16}$/
    }
}
