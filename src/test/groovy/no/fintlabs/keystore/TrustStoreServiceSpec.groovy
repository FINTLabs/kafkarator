package no.fintlabs.keystore

import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Specification

import static no.fintlabs.keystore.TestUtil.isBase64

class TrustStoreServiceSpec extends Specification {

    def ca = "-----BEGIN CERTIFICATE-----\n" +
            "MIIEQTCCAqmgAwIBAgIUYHAflDHsu8t4c/xhS7MHHMGPD/AwDQYJKoZIhvcNAQEM\n" +
            "BQAwOjE4MDYGA1UEAwwvOGEwNTk4NmEtOTA5Ni00MDY2LTk0NjEtMGVkZTNlM2Uy\n" +
            "NTQ1IFByb2plY3QgQ0EwHhcNMjIxMDA1MDkwOTU1WhcNMzIxMDAyMDkwOTU1WjA6\n" +
            "MTgwNgYDVQQDDC84YTA1OTg2YS05MDk2LTQwNjYtOTQ2MS0wZWRlM2UzZTI1NDUg\n" +
            "UHJvamVjdCBDQTCCAaIwDQYJKoZIhvcNAQEBBQADggGPADCCAYoCggGBAMhow9jX\n" +
            "VIwZdEev/PY8brUm2fuJtgMxIcH3H/FSa0B4vxSue59RgX1CvLeCWqbu63LBCO45\n" +
            "sf1imqnIGhQJkXnAfw+hUJ01SwdZUEi6VQcxVaKCQUSNUmb/jfuF2/MHFPVCeYi7\n" +
            "k20G5HtIF1fiyBx2sl2N3PqFhnBm5iV1QUNsZMBVOTMKtblWPdaNMc8DntSHsjAM\n" +
            "1Xv0XtcOp7bWbndhav86H7L1AGnFVJexlvCH/G++PId6SqlWn4IfIVqKBidy1Uie\n" +
            "fQWTZlTIebUU/qZioc4KXovjgpbj9+wS23Fc/iAjb5pDAi48xORcD7Uf4F4DNO2Z\n" +
            "sJ/YAWWl7fBpI5spDAho1k40t1KnKIyfp+/162K9hd+WRkJtuK/xCDKxYMlsGfgy\n" +
            "SrSDLFEMXaOFdK5hHHkRRZFvipGnca3WLMykyTDkXkyR+OmDMveNe7vmDCqxzqmg\n" +
            "JIboTgEULzKbaPbsAlYgmkNLRs17/A7hhimxulBWOiLgyopsHJdt8ZoSJQIDAQAB\n" +
            "oz8wPTAdBgNVHQ4EFgQUzqPYk8EK2frnDMVRxamLbGifL3EwDwYDVR0TBAgwBgEB\n" +
            "/wIBADALBgNVHQ8EBAMCAQYwDQYJKoZIhvcNAQEMBQADggGBAG/KYxeMlLAqYnQU\n" +
            "g6EonqQRRepoxRasqLt79gqD9nGlqJ9PWN8YyHxzItqVr67pkXyXf+7SXqFx6NPf\n" +
            "FhvbzusjqzY3baay7eVS3V6TVURdI3/BN4m0OIaavE1jGRCF6M6+yMnbwnwj1inp\n" +
            "LXP/IlKAagnLwPYLgKDCFCvw6oN8r7OeRp4uu0cgScKwK3t/PA2O/talxRdURKvl\n" +
            "Aw9V7DaGfHctPb7VN3g57FdSGGuC49FyoktqAfibrtE5bWlrL1wPpEtvEPuZm8sO\n" +
            "09l1ZnXvS/ggyWV4HgTeHhh4IsSfPVLqLg36rhm5XN0Wv9qQMnLbfaswNyHYjix3\n" +
            "IPppVrrdPSHsynsU4yo/7ljkljQBAwHoeAhkOKpnZVPdnl5ppUbDMO0edutUftNE\n" +
            "nt9nqkqvWu4CbX0bOnJmQIDs0e7m97Lm19HjTEUV26rJCw9hMATdIUx9N+c/UbSH\n" +
            "FmdxEWZ8/1AW9yOQji7poBPuh/UNysK4jPdrDvnXpQv1jSlHbg==\n" +
            "-----END CERTIFICATE-----\n"

    def "There is 1 entry in the trust store"() {
        given:
        def store = new TrustStoreService()

        when:
        def trustStore = store.createTrustStore(
                ca
        )

        then:
        trustStore.size() == 1
    }

    def "The trust store is of type JKS"() {
        given:
        def trustStore = new TrustStoreService()

        when:
        def store = trustStore.createTrustStore(
                ca
        )

        then:
        store.getType().toLowerCase() == "JKS".toLowerCase()
    }

    def "aiven ca should exist in trust store"() {
        given:
        def service = new TrustStoreService()
        def store = service.createTrustStore(
                ca
        )

        when:
        def caCertExists = store.containsAlias("aiven ca",)

        then:
        caCertExists
    }

    def "Convert trust store to base64"() {
        given:
        def service = new TrustStoreService()
        def password = RandomStringUtils.randomAscii(32).toCharArray()

        when:
        def store = service.storeToBase64(service.createTrustStore(
                ca
        ), password)
        def base64 = isBase64(store)

        then:
        store
        base64
    }
}
