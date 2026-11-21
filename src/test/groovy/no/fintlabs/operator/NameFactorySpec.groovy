package no.fintlabs.operator

import spock.lang.Specification
import spock.lang.Unroll

class NameFactorySpec extends Specification {

    def "Name should contain sanitized orgId, team, appName and hash"() {
        when:
        def name = NameFactory.serviceUserName("flais.io", "flais", "fint-data-service")

        then:
        name ==~ /flais_io_flais_fint-data-service_[a-f0-9]{8}/
    }

    def "Name should replace dots with underscore"() {
        when:
        def name = NameFactory.serviceUserName("flais.io", "flais", "fint.data.service")

        then:
        name ==~ /flais_io_flais_fint_data_service_[a-f0-9]{8}/
        !name.contains(".")
    }

    def "Name should remove team prefix from team name"() {
        when:
        def name = NameFactory.serviceUserName("flais.io", "teamflais", "fint-data-service")

        then:
        name ==~ /flais_io_flais_fint-data-service_[a-f0-9]{8}/
    }

    def "Name should remove _no suffix from org name"() {
        when:
        def name = NameFactory.serviceUserName("flais_no", "flais", "fint-data-service")

        then:
        name ==~ /flais_flais_fint-data-service_[a-f0-9]{8}/
    }

    def "Name should remove team name prefix from app name"() {
        when:
        def name = NameFactory.serviceUserName("flais.io", "team-flais", "team-flais-service")

        then:
        name ==~ /flais_io_flais_service_[a-f0-9]{8}/
    }

    def "Name should shorten long values"() {
        when:
        def name = NameFactory.serviceUserName(
                "very-long-org-name.io",
                "even-longer-team-name-here",
                "very-long-application-name-service"
        )

        then:
        name ==~ /very-long_even-longer-tea_very-long-application-nam_[a-f0-9]{8}/
    }

    def "Name should be deterministic"() {
        when:
        def name1 = NameFactory.serviceUserName("flais.io", "flais", "fint-data-service")
        def name2 = NameFactory.serviceUserName("flais.io", "flais", "fint-data-service")

        then:
        name1 == name2
    }

    def "Different inpup should produce different hash"() {
        when:
        def name1 = NameFactory.serviceUserName("flais.io", "flais", "fint-data-service")
        def name2 = NameFactory.serviceUserName("flais.io", "flais", "novari-data-service")

        then:
        name1 != name2
    }

    @Unroll
    def "Name should sanitize '#orgName', '#teamName', '#appName'"() {
        when:
        def name = NameFactory.serviceUserName(orgName, teamName, appName)

        then:
        name ==~ expected

        where:
        orgName     | teamName     | appName             || expected
        "flais.io"  | "flais"      | "app"               || /flais_io_flais_app_[a-f0-9]{8}/
        "foo.bar"   | "teamalpha"  | "alpha-api"         || /foo_bar_alpha_alpha-api_[a-f0-9]{8}/
        "kunde_no"  | "teambeta"   | "beta-service"      || /kunde_beta_beta-service_[a-f0-9]{8}/
        "-test.io"  | "-teamgamma" | "-gamma-service-"   || /test_io_teamgamma_gamma-service_[a-f0-9]{8}/
    }

    def "userName should use legacy username when annotation is missing"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "test.io")
        crd.getMetadata().getLabels().put("fintlabs.no/team", "test")
        crd.getMetadata().setName("test-data-service")

        when:
        def name = NameFactory.userName(crd)

        then:
        name == "test-io_test_test-data-service"

    }

    def "userName should use legacy username when annotations is null"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().setAnnotations(null)
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "test.io")
        crd.getMetadata().getLabels().put("fintlabs.no/team", "test")
        crd.getMetadata().setName("test-data-service")

        when:
        def name = NameFactory.userName(crd)

        then:
        name == "test-io_test_test-data-service"
    }

    def "userName should use legacy username when annotation is not v2"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "test.io")
        crd.getMetadata().getLabels().put("fintlabs.no/team", "test")
        crd.getMetadata().setName("test-data-service")
        crd.getMetadata().getAnnotations().put("kafka.fintlabs.no/name-version", "v1")

        when:
        def name = NameFactory.userName(crd)

        then:
        name == "test-io_test_test-data-service"
    }

    def "userName should use v2 username when annotation is v2"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "test.io")
        crd.getMetadata().getLabels().put("fintlabs.no/team", "test")
        crd.getMetadata().setName("test-data-service")
        crd.getMetadata().getAnnotations().put("kafka.fintlabs.no/name-version", "v2")

        when:
        def name = NameFactory.userName(crd)
        then:
        name ==~ /test_io_test_data-service_[a-f0-9]{8}/
    }

    def "legacyUserName should replace dots with hyphen"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        crd.getMetadata().getLabels().put("fintlabs.no/org-id", "test.io")
        crd.getMetadata().getLabels().put("fintlabs.no/team", "test")
        crd.getMetadata().setName("test-data-service")

        expect:
        NameFactory.legacyUserName(crd) == "test-io_test_test-data-service"
    }

    def "userName should not mutate annotations when annotations are null"() {
        given:
        def resource = new KafkaUserAndAclCrd()
        resource.getMetadata().setAnnotations(null)
        resource.getMetadata().setName("test-data-service")
        resource.getMetadata().getLabels().put("fintlabs.no/org-id", "test.io")
        resource.getMetadata().getLabels().put("fintlabs.no/team", "test")

        when:
        def name = NameFactory.userName(resource)

        then:
        name == "test-io_test_test-data-service"
        resource.getMetadata().getAnnotations() == null
    }
}
