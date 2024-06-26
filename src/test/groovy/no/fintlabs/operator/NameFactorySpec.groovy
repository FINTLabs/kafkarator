package no.fintlabs.operator

import spock.lang.Specification

class NameFactorySpec extends Specification {

    def "Name should contain orgId and team"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/team", "flais")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "flais.io")
        crd.getMetadata().setName("fint-data-service")

        when:
        def name = NameFactory.generateNameFromMetadata(crd)

        then:
        name == "flais-io_flais_fint-data-service"
    }

    def "Name should be shortened if too long"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/team", "flais")
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "flais.io")
        crd.getMetadata().setName("flais-io_flais_fint-data-service-that-is-even-longer-than-64-characters")

        when:
        def name = NameFactory.generateNameFromMetadata(crd)
        System.out.println(name)

        then:
        name == "flais_fint-data-service-that-is-even-longer-than-64-characters"
    }


    def "check name length"() {
        when:
        def name = "flais_fint-data-service-that-is-even-longer-than-64-characters"

        then:
        System.out.println(name.length())
    }
}
