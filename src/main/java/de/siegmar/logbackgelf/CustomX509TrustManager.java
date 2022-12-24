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

import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.X509TrustManager;

class CustomX509TrustManager implements X509TrustManager {

    private static final int TYPE_DNS_NAME = 2;

    private final X509TrustManager trustManager;
    private final String hostname;

    CustomX509TrustManager(final X509TrustManager trustManager, final String hostname) {
        this.trustManager = trustManager;
        this.hostname = hostname;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType)
        throws CertificateException {

        // First, check the chain via the trust manager
        trustManager.checkServerTrusted(chain, authType);

        final X509Certificate serverCert = chain[0];

        if (checkAlternativeNames(serverCert)) {
            return;
        }

        // Check the deprecated common name
        checkCommonName(serverCert);
    }

    private boolean checkAlternativeNames(final X509Certificate serverCert)
        throws CertificateException {

        final List<String> alternativeNames = getAlternativeNames(serverCert);
        if (!alternativeNames.isEmpty()) {
            for (final String alternativeName : alternativeNames) {
                if (HostnameVerifier.verify(hostname, alternativeName)) {
                    return true;
                }
            }

            throw new CertificateException(String.format("Server certificate mismatch. Tried to "
                + "verify %s against subject alternative names: %s", hostname, alternativeNames));
        }
        return false;
    }

    private static List<String> getAlternativeNames(final X509Certificate cert)
        throws CertificateParsingException {

        final Collection<List<?>> subjectAlternativeNames = cert.getSubjectAlternativeNames();
        if (subjectAlternativeNames == null) {
            return Collections.emptyList();
        }

        final List<String> ret = new ArrayList<>();
        for (final List<?> entry : subjectAlternativeNames) {
            if (((Integer) entry.get(0)) == TYPE_DNS_NAME) {
                final String dNSName = (String) entry.get(1);
                if (dNSName != null) {
                    ret.add(dNSName);
                }
            }
        }

        return ret;
    }

    private void checkCommonName(final X509Certificate serverCert) throws CertificateException {
        final String commonName;
        try {
            commonName = getCommonName(serverCert);
            if (HostnameVerifier.verify(hostname, commonName)) {
                return;
            }
        } catch (final InvalidNameException e) {
            throw new CertificateException("Could not read CN from certificate", e);
        }

        throw new CertificateException(String.format("Server certificate mismatch. "
            + "Tried to verify %s against common name: %s", hostname, commonName));
    }

    private static String getCommonName(final X509Certificate cert) throws InvalidNameException {
        final LdapName ldapName = new LdapName(cert.getSubjectDN().getName());
        for (final Rdn rdn : ldapName.getRdns()) {
            if ("CN".equalsIgnoreCase(rdn.getType())) {
                return (String) rdn.getValue();
            }
        }

        throw new InvalidNameException("No common name found");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return trustManager.getAcceptedIssuers();
    }

}
