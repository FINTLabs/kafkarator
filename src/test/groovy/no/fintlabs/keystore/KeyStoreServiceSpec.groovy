package no.fintlabs.keystore

import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Specification

import java.security.UnrecoverableKeyException

import static no.fintlabs.keystore.TestUtil.isBase64

class KeyStoreServiceSpec extends Specification {

    def cert = "-----BEGIN CERTIFICATE-----\n" +
            "MIIEPTCCAqWgAwIBAgIUFCHYIvTx5Di8Hqth9SF7B9K+mRowDQYJKoZIhvcNAQEM\n" +
            "BQAwOjE4MDYGA1UEAwwvOGEwNTk4NmEtOTA5Ni00MDY2LTk0NjEtMGVkZTNlM2Uy\n" +
            "NTQ1IFByb2plY3QgQ0EwHhcNMjIxMTE4MjAyNzA0WhcNMjUwMjE1MjAyNzA0WjA8\n" +
            "MRQwEgYDVQQKDAtrYWZrYS1hbHBoYTERMA8GA1UECwwIdTh5OHI1aTIxETAPBgNV\n" +
            "BAMMCHRlc3R1c2VyMIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEA9RdD\n" +
            "/H3WMBxwHgKf3FajXseP+XyiTRf+LgIDys7fq1xP+whHmeHz/7g1TA7nckYsCee2\n" +
            "tSRq6Azjyn9LMGtrfLOE9nWIhtCd+uZxOiWIi8Tji6rlH75st+8UxPmRtGmTywOa\n" +
            "DzWhKhIyPvROZmnI3bpydEJaLyousKT4TCHRdU7AjP1BW+zpVdUvhyGRaEMoeFSA\n" +
            "p0OyPwC7G0yP4PxTJJWG2HAoFL4f0P/H5vITdxnYI2nKyYiBWAfvdWtJ98VLKDvk\n" +
            "fl/b7auIniTkzU5gtCD2cAO9O4cE93gqq/eHvSDBZ7VRoozZH/9SeKBCOXm4FS5y\n" +
            "C2VVP2iLzMASF5bYVhhtjBQxJn+TqjHngO2ci4se/9VAvDkRKnvAawxhCFPBbYSC\n" +
            "XaVU1Xv/fHr8YdJE1PNc/FHNzlijjsvOZ7AIA7H/t2F9n176eYqQt+XglZmfBFuu\n" +
            "fzejESUVfyoKEzqSZ3CHd8wVT96ZYnXhXrm7S3OIgUPqv+AG5eTjDWLSNR0fAgMB\n" +
            "AAGjOTA3MB0GA1UdDgQWBBT+pZa+/wNQCttXWDS2Txc2haLi1DAJBgNVHRMEAjAA\n" +
            "MAsGA1UdDwQEAwIFoDANBgkqhkiG9w0BAQwFAAOCAYEAQIi8q1HvDLStPQQ3kBpl\n" +
            "j0hDaezwG1O7Ti2brN+htkLbnlGPh14XbQNiLXIc+i1jaZaCoLhDa+zV5gIn0BGF\n" +
            "Et8o+VGWFppOVbqc5yn/mzd0wv8qkkEyxEPGj4ehmJZ2Qw3SiuIQm+n8FyMZiqIE\n" +
            "wVFLcKpOgFbBBLifyyChasx9kujMxYov+Vh5giOXB2fMO775R8mhF/gtJqLkdzb+\n" +
            "ukIxaAFvYmAxJi3BMUfjgb8NysMzbnAyYFAwB+cCmwYU7eqScmrGy7gHrP+SwJoL\n" +
            "Cwvc5Ybt8YWS+LpO3raMIgJH3vcrcPOQwvv9D1oSazzrhhJDtLgQOv/uefVHUIkh\n" +
            "GVhGrrZn56XHxVhwTqU8j7LWOuUPng+BU7IXQfgDY2aIW5CsXjC4A7tVejlm6Omq\n" +
            "t0eWzuBsHbcEF7WqWjR35eAJCPDr+HSEjH+Yt+tQpTpb7EbY/Uld25VNz1TqcjdN\n" +
            "0alYdUkDoenBIHhUKX/hVgfgFA1aoXdneNwuRJD/BzQJ\n" +
            "-----END CERTIFICATE-----"
    def key = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIG/AIBADANBgkqhkiG9w0BAQEFAASCBuYwggbiAgEAAoIBgQD1F0P8fdYwHHAe\n" +
            "Ap/cVqNex4/5fKJNF/4uAgPKzt+rXE/7CEeZ4fP/uDVMDudyRiwJ57a1JGroDOPK\n" +
            "f0swa2t8s4T2dYiG0J365nE6JYiLxOOLquUfvmy37xTE+ZG0aZPLA5oPNaEqEjI+\n" +
            "9E5macjdunJ0QlovKi6wpPhMIdF1TsCM/UFb7OlV1S+HIZFoQyh4VICnQ7I/ALsb\n" +
            "TI/g/FMklYbYcCgUvh/Q/8fm8hN3GdgjacrJiIFYB+91a0n3xUsoO+R+X9vtq4ie\n" +
            "JOTNTmC0IPZwA707hwT3eCqr94e9IMFntVGijNkf/1J4oEI5ebgVLnILZVU/aIvM\n" +
            "wBIXlthWGG2MFDEmf5OqMeeA7ZyLix7/1UC8OREqe8BrDGEIU8FthIJdpVTVe/98\n" +
            "evxh0kTU81z8Uc3OWKOOy85nsAgDsf+3YX2fXvp5ipC35eCVmZ8EW65/N6MRJRV/\n" +
            "KgoTOpJncId3zBVP3plideFeubtLc4iBQ+q/4Abl5OMNYtI1HR8CAwEAAQKCAYB0\n" +
            "AqA2qqWwvjU/r0RV+lh4K4P1ts3oKfwxcNi0rblnlllxXJjvuOlS2LG5n7BkAd0k\n" +
            "jx6HnYZQ8pD8L8a6U45cR2kfc8C895Bfy6Y6vLtmVTrTyH+NyAgUBEHTfsI+IF9J\n" +
            "kBnjKyJvFI/eBqv5hAcFFRTJ/QJNIu1Yxa2V88f+e9leox3F0l0cOL23e+ck7+z/\n" +
            "EBiZk8yST9lA8buE5krAnVpnpqcCMoBC4F+R7HHURVJuaMwJGukGbzMkZDVt519f\n" +
            "tMez+hqCA9GGzqTViwdj5fS8Hgx8iJ5yZQLZ5rdBIQRXA6Hm9lQudNhvRLhzgOhJ\n" +
            "IjUpGaiN+Q92RKzCL4N06k8w/yVZb3zHPSPfd0eDWQWCMChIUerr+tE1jX84fn9o\n" +
            "xye7kbVMJnL8XtGIc8Q5kTaNfCIG8cvJdFPl2XYxAJtcPIik13XhK94LGKk9KsCt\n" +
            "Zs5g51288rIwAUKdaxLeIdbwjGCRmZLJkZl0HNlMujj5W9h1v0in0d8W+mqiNuEC\n" +
            "gcEA/ZFtcoKwboSnJxh+gssQCuFO0jtRUdLQRVgTx+Wcb6FiyXxNbCu3aa0xvTXS\n" +
            "6F7TdSjneLccBBcWkbRNIOLNCgcTHUewmk3c+PBF1NvIyD3BO5vcns2hZnAAPS48\n" +
            "fSoawgc94wQ1TcTZWI0abTeRTHwRXxLJN3QOZea5EdhAa6Bg6osK0IPrFSqVz+Wg\n" +
            "Uwu7AIr6FinW26lYVZXxH9ZPIgvav8dtFU+9RR3JxZivZzlaTHcL76XmDgQ0+2hl\n" +
            "qpJRAoHBAPdxBkDneSbIHPOzXdjk3j29pfcZ6OROUipPg6z9AY+Y/Vqo8gkavKVa\n" +
            "fywmyqdCBy2H76vm0Em9CraXtvGiUADyi78/2jS6Qd9liYD/rAOI+Z75q3p29H2+\n" +
            "xz+c7OF4KnfGUG5OB7TXkEbjaTpRmGVsIPfLft2eOWo4cmncRMe8nekBFwmUVraH\n" +
            "x672WoEXcbeEYz3OBojpi5NlcwmTrdu7IqCBGGFLQrdqI+FV8xziavsd8bTnvZLK\n" +
            "tm5kvE3sbwKBwEF5+xQVsS9oq60JwylM/ECC+5KH5LDHSiaN3tXAccvVlafHpEpp\n" +
            "Y3wzT+O28VY1nk7jmBnw1pgryhUnxL7YMfAD8aie6Xh+4K+bg7y6YDds0ufskGSl\n" +
            "XqcQBs/Fj7MWW0B9FDr5zDXfvewCy68mKSvh4a52MGVUX59qm6GDAOeJy1zlOI6t\n" +
            "cdfOXk8H0YWAQvKZ8Igml6ezK+81v8Tj1x1IiFz7Ryy4oXwmfAK9MZXrFmkmWHuy\n" +
            "kBxQc8nzpEUOcQKBwC8W21HOu0eeHaRjJiD4rTKivMrhq2iHXyueJjjtjTZ+rvhp\n" +
            "3mXNKMlvB3SnjTl3X1ZS4FGeg1UFHNzS0oX5lbAYeG/U/vm52H9jG1/pSBsEHj0a\n" +
            "1n9f0/d6LwD0JEBkVfClVPko8F6CB3r2HDh0/sOOaFe/6kzSp6qHQ37R5GT+iC6n\n" +
            "fnLcQARMoruxNlgK1iFOmHRSmPvFNkK+s73G1PdUDart5m3dnLy6/kkLylBc5m8T\n" +
            "q8P5tiIRYdZwZUWPEQKBwCWgUgoUFOvAIyIQYPggL1hqb8c7ZJ8spKeMajU5QNw3\n" +
            "ji5P6aTDVWnc4ZmQcDj/jXiUUc9YDCZUevNfJeYAyyQLgHpc42myzUh7LqjX+toG\n" +
            "bkNLsGuZPgt+TGEKd95GrHW064KUOKxO79VJKp2lniIkZJCoEzP29Y/41pHbScWO\n" +
            "fE7MW6kDvvZD9vGfDIpJO0K5Bn0xc/Fw3T5hFR4jSQYeyW73bv70IbfkXFYhN2a+\n" +
            "R/VhQn+q7bNLhr5aEdX+hw==\n" +
            "-----END PRIVATE KEY-----"
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

    def "There is 1 entry in the keystore"() {
        given:
        def service = new KeyStoreService()
        def password = RandomStringUtils.randomAscii(32).toCharArray()

        when:
        def store = service.createKeyStore(cert,
                key,
                ca,
                password
        )

        then:
        store.size() == 1
    }

    def "The keystore is of type PKCS12"() {
        given:
        def service = new KeyStoreService()
        def password = RandomStringUtils.randomAscii(32).toCharArray()

        when:
        def store = service.createKeyStore(cert,
                key,
                ca,
                password
        )

        then:
        store.getType() == "PKCS12"
    }

    def "When getting the private key with the correct password no exceptions should be thrown and the key should not be null"() {
        given:
        def service = new KeyStoreService()
        def password = RandomStringUtils.randomAscii(32).toCharArray()
        def store = service.createKeyStore(
                cert,
                key,
                ca,
                password
        )

        when:
        def privateKey = store.getKey("1", password)

        then:
        privateKey != null
        notThrown(UnrecoverableKeyException.class)
    }

    def "Convert keystore to base64"() {
        given:
        def service = new KeyStoreService()
        def password = RandomStringUtils.randomAscii(32).toCharArray()

        when:
        def store = service.storeToBase64(service.createKeyStore(cert,
                key,
                ca,
                password
        ), password)
        def base64 = isBase64(store)

        then:
        store
        base64
    }
}
