/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2016 Oliver Siegmar
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

import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class GelfTcpTlsAppender extends GelfTcpAppender {

    /**
     * If {@code true}, skip the TLS certificate validation.
     */
    private boolean insecure;

    public boolean isInsecure() {
        return insecure;
    }

    public void setInsecure(final boolean insecure) {
        this.insecure = insecure;
    }

    @Override
    protected SSLSocketFactory initSocketFactory() {
        try {
            return configureSslFactory(newTrustManager());
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private TrustManager newTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        if (insecure) {
            addWarn("Enabled insecure mode (skip TLS certificate validation)"
                + " - don't use this in production!");
            return new NoopX509TrustManager();
        }

        return new CustomX509TrustManager(findDefaultX509TrustManager(), getGraylogHost());
    }

    private static X509TrustManager findDefaultX509TrustManager()
        throws NoSuchAlgorithmException, KeyStoreException {

        final TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);

        for (final TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }

        throw new IllegalStateException("No X509 TrustManager found");
    }

    private SSLSocketFactory configureSslFactory(final TrustManager trustManager)
        throws NoSuchAlgorithmException, KeyManagementException {

        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        return context.getSocketFactory();
    }

}
