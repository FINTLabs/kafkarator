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
        def name = NameFactory.nameFromMetadata(crd)

        then:
        name == "flais-io_flais_fint-data-service"
    }
}
