package no.fintlabs.operator

import spock.lang.Specification
import spock.lang.Subject

class NameFactorySpec extends Specification {

    @Subject
    NameFactory nameFactory = new NameFactory()

    def "Test generateNameFromMetadata within 64 characters"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().setName("fint-data-service")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "flais.io")
        crd.getMetadata().getLabels().put("fintlabs.no/team", "flais")

        when:
        String result = nameFactory.generateNameFromMetadata(crd)

        then:
        result == "flais-io_flais_fint-data-service"
    }

    def "Test generateNameFromMetadata longer than 64 and contains io"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().setName("data-service-that-contains-io_even-longer-than-64-characters-and")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "")
        crd.getMetadata().getLabels().put("fintlabs.no/team", "")

        when:
        String result = nameFactory.generateNameFromMetadata(crd)

        then:
        result == "data-service-that-contains-even-longer-than-64-characters-and"
    }

    def "Test generateNameFromMetadata longer than 64 and contains team"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().setName("fint-data-service-that-is-longer-than-64-characters-with-team")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "")
        crd.getMetadata().getLabels().put("fintlabs.no/team", "flais")

        when:
        String result = nameFactory.generateNameFromMetadata(crd)
        System.out.println(result)

        then:
        result == "fint-data-service-that-is-longer-than-64-characters-with-team"

    }

    def "Test generateNameFromMetadata longer than 64 and contains orgId"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().setName("fint-data-service-that-is-longer-than-64-characters-with-orgId")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "flais.io")
        crd.getMetadata().getLabels().put("fintlabs.no/team", "")

        when:
        String result = nameFactory.generateNameFromMetadata(crd)

        then:
        result == "fint-data-service-that-is-longer-than-64-characters-with-orgId"
    }

    def "Name should be shortened if too long"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/team", "flais")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "flais.io")
        crd.getMetadata().setName("fint-data-service-that-is-even-longer-than-64-characters")

        when:
        def result = NameFactory.generateNameFromMetadata(crd)

        then:
        result == "flais-flais_fint-data-service-that-is-even-longer-than-64-characters"
    }

    def "Team is null if not present"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "flais.io")
        crd.getMetadata().setName("fint-data-service")

        when:
        def result = NameFactory.generateNameFromMetadata(crd)

        then:
        result == "flais-io_null_fint-data-service"
    }

    def "Name is null if not present"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/team", "flais")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "flais.io")

        when:
        def result = NameFactory.generateNameFromMetadata(crd)

        then:
        result == "flais-io_flais_null"
    }

    def "Fails if orgId is null"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/team", "flais")
        crd.getMetadata().setName("fint-data-service")

        when:
        def result = NameFactory.generateNameFromMetadata(crd)

        then:
        thrown(NullPointerException)
    }
}
