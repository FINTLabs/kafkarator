package no.fintlabs.operator

import spock.lang.Specification

class NameFactorySpec extends Specification {

    def "Legacy should include orgId, team and name"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/team", "test-team")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "flais.io")
        crd.getMetadata().setName("fint-data-service")

        when:
        def name = NameFactory.legacyNameFromMetadata(crd)

        then:
        name.equals("flais-io_test-team_fint-data-service")
    }

    def "guidNameFromMetadata should return valid UUID format"() {
        when:
        def guid = NameFactory.guidNameFromMetadata()

        then:
        guid.matches(/[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}/)
        guid.length() <= 64
    }

    def "nameFromMetadata should return GUID name is use-guid is set to true"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/use-guid", "true")

        when:
        def name = NameFactory.nameFromMetadata(crd)

        then:
        name.matches(/[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}/)
    }

    def "nameFromMetadata should return legacy name if use-guid is not true"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/team", "test-team")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "flais.io")
        crd.getMetadata().getLabels().put("fintlabs.no/use-guid", "false")
        crd.getMetadata().setName("fint-data-service")

        when:
        def name = NameFactory.nameFromMetadata(crd)

        then:
        name.equals("flais-io_test-team_fint-data-service")
    }
}
