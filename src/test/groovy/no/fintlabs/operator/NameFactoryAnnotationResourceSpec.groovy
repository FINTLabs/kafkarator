package no.fintlabs.operator

import io.fabric8.kubernetes.api.model.ObjectMeta
import spock.lang.Specification
import spock.lang.Subject

import io.javaoperatorsdk.operator.api.reconciler.Context

class NameFactoryAnnotationResourceSpec extends Specification{

    @Subject
    def resource = new NameFactoryAnnotationResource()

    def "should generate legacy username and add it as annotation"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        def metadata = new ObjectMeta()
        metadata.name = "test-resource"
        metadata.labels = [
                "fintlabs.no/org-id": "flais.io",
                "fintlabs.no/team": "flais"
        ]
        crd.metadata = metadata

        and:
        def context = Mock(Context)

        when:
        def result = resource.reconcile(crd, context)

        then:
        def annotations = crd.metadata.annotations
        annotations != null
        annotations["fintlabs.no/generated-username"] == "flais-io_flais_test-resource"
        result != null
        result.resourceOperations.size() == 0
    }

    def "should create GUID when use-guid is true"() {
        given:
        def crd = new KafkaUserAndAclCrd()
        def metadata = new ObjectMeta()
        metadata.name = "test-resource"
        metadata.labels = [
                "fintlabs.no/org-id": "flais.io",
                "fintlabs.no/team": "flais",
                "fintlabs.no/use-guid": "true"
        ]
        crd.metadata = metadata

        and:
        def context = Mock(Context)

        when:
        def result = resource.reconcile(crd, context)

        then:
        def annotations = crd.metadata.annotations
        annotations != null
        def generated = annotations["fintlabs.no/generated-username"]
        generated != null
        !(generated.contains("flais"))
        generated ==~ /^[0-9a-fA-F\-]{36}$/
        result != null
        result.resourceOperations.size() == 0
    }
}
