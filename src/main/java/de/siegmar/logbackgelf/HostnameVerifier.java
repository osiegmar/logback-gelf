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

import java.util.Locale;
import java.util.Objects;

final class HostnameVerifier {

    private HostnameVerifier() {
    }

    /**
     * Hostname verification logic based on RFC 6125.
     * <p>
     * If using a wildcard, the wildcard has to be the left-most label (e.g. "*.foo.com").
     *
     * @param hostname the hostname to validate (e.g. "bar.foo.com")
     * @param certname the name (CN or SubjectAlternativeName) from the certificate to verify
     *                 against (e.g. "bar.foo.com" or "*.foo.com")
     * @return {@code true}, if the hostname is verified.
     * @throws NullPointerException if {@code hostname} or {@code certname} is null.
     */
    public static boolean verify(final String hostname, final String certname) {
        final String host = Objects.requireNonNull(hostname).toLowerCase(Locale.ENGLISH);
        final String cert = Objects.requireNonNull(certname).toLowerCase(Locale.ENGLISH);

        if (cert.startsWith("*.")) {
            // *.foo.com becomes foo.com
            final String cnSuffix = cert.substring(2);

            // At least two labels -- no TLD alone
            if (!cnSuffix.contains(".")) {
                return false;
            }

            // bar.foo.com becomes foo.com
            final String hostSuffix = host.substring(host.indexOf('.') + 1);

            return cnSuffix.equals(hostSuffix);
        }

        // Simple case, exact match
        return host.equals(cert);
    }

}
