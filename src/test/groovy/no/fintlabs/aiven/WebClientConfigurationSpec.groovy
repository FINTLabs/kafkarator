package no.fintlabs.aiven

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class WebClientConfigurationSpec extends Specification {

    private MockWebServer mockWebServer

    def setup() {
        mockWebServer = new MockWebServer()
        mockWebServer.start()
    }

    def cleanup() {
        mockWebServer.shutdown()
    }

    def "webClient adds bearer auth header to outgoing requests"() {
        given:
        def properties = new AivenProperties()
        properties.setBaseUrl(mockWebServer.url("/").toString())
        properties.setToken("super-secret-token")
        def webClient = new WebClientConfiguration(properties).webClient()
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody('{ "ok": true }'))

        when:
        webClient.get()
                .uri("/project/test")
                .retrieve()
                .bodyToMono(String)
                .block()

        then:
        with(takeRequest()) {
            method == "GET"
            path == "/project/test"
            getHeader("Authorization") == "Bearer super-secret-token"
        }
    }

    private RecordedRequest takeRequest() {
        mockWebServer.takeRequest(1, TimeUnit.SECONDS)
    }
}
