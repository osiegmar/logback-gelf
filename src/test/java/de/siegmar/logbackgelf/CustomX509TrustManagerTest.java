/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2019 Oliver Siegmar
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.siegmar.logbackgelf;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Arrays;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomX509TrustManagerTest {

    private static final String HOSTNAME = "graylog.foo.bar";

    private CustomX509TrustManager tm =
        new CustomX509TrustManager(defaultTrustManager(null), HOSTNAME);
    private X509Util.CertBuilder c;

    @BeforeEach
    public void before() throws Exception {
        c = new X509Util.CertBuilder();
    }

    @SuppressWarnings("checkstyle:illegalcatch")
    @Test
    public void selfSigned() {
        // The default trust manager will reject a self-signed certificate
        try {
            validate(c.build(null, HOSTNAME));
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("unable to find valid certification path"));
        }

        // Using the NoopX509TrustManager the same certificate will be allowed
        tm = new CustomX509TrustManager(new NoopX509TrustManager(), HOSTNAME);
        assertDoesNotThrow(() -> validate(c.build(null, HOSTNAME)));
    }

    @Test
    public void validCert() throws Exception {
        tm = new CustomX509TrustManager(new NoopX509TrustManager(), HOSTNAME);

        validate(c.build(null, HOSTNAME));
        validate(c.build(null, "x.foo.bar", HOSTNAME));
        validate(c.build(null, "x.foo.bar", "*.foo.bar"));
        validate(c.build(HOSTNAME));
    }

    @Test
    public void nameMismatch() {
        tm = new CustomX509TrustManager(new NoopX509TrustManager(), HOSTNAME);

        final String wrongName = "graylog2.foo.bar";

        CertificateException e = assertThrows(CertificateException.class,
            () -> validate(c.build(null, wrongName)));
        assertEquals("Server certificate mismatch. Tried to verify graylog.foo.bar "
            + "against subject alternative names: [graylog2.foo.bar]", e.getMessage());

        e = assertThrows(CertificateException.class, () -> validate(c.build(wrongName)));
        assertEquals("Server certificate mismatch. Tried to verify graylog.foo.bar "
            + "against common name: graylog2.foo.bar", e.getMessage());
    }

    @Test
    public void caSigned() throws Exception {
        final X509Util.CABuilder caBuilder = new X509Util.CABuilder();
        final X509Certificate cert = prepareCaSigned(caBuilder)
            .build(HOSTNAME);

        validate(cert, caBuilder.getCaCertificate());
    }

    @Test
    void clientValidation() {
        assertThrows(UnsupportedOperationException.class,
            () -> tm.checkClientTrusted(new X509Certificate[] {}, "RSA"));
    }

    @Test
    void acceptedIssuers() {
        assertTrue(Arrays.equals(defaultTrustManager(null).getAcceptedIssuers(), tm.getAcceptedIssuers()));
    }

    private X509Util.CertBuilder prepareCaSigned(final X509Util.CABuilder caBuilder)
        throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {

        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("my-cert", caBuilder.getCaCertificate());

        tm = new CustomX509TrustManager(defaultTrustManager(ks), HOSTNAME);

        return c
            .caPrivateKey(caBuilder.getKeyPair().getPrivate())
            .caCertificate(caBuilder.getCaCertificate());
    }

    @Test
    public void expired() throws Exception {
        final X509Util.CABuilder caBuilder = new X509Util.CABuilder();
        final X509Certificate cert = prepareCaSigned(caBuilder)
            .validFrom(LocalDate.now().minusYears(2))
            .validTo(LocalDate.now().minusYears(1))
            .build(HOSTNAME);

        assertThrows(CertificateException.class,
            () -> validate(cert, caBuilder.getCaCertificate()));
    }

    private void validate(final X509Certificate... certificates) throws CertificateException {
        tm.checkServerTrusted(certificates, "RSA");
    }

    private static X509TrustManager defaultTrustManager(final KeyStore keyStore) {
        try {
            final TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            for (final TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    return (X509TrustManager) trustManager;
                }
            }

            return null;
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new IllegalStateException(e);
        }
    }

}
