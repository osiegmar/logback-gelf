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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * GELF HTTP Appender.
 */
public class GelfHttpAppender extends AbstractGelfAppender {

    private static final int DEFAULT_CONNECT_TIMEOUT = 15_000;
    private static final int DEFAULT_READ_TIMEOUT = 5_000;
    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final int DEFAULT_RETRY_DELAY = 3_000;

    private String url;
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int readTimeout = DEFAULT_READ_TIMEOUT;
    private boolean insecure;
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private int retryDelay = DEFAULT_RETRY_DELAY;
    private CompressionMethod compressionMethod = CompressionMethod.GZIP;
    private SSLSocketFactory sslSocketFactory;

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(final int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isInsecure() {
        return insecure;
    }

    public void setInsecure(final boolean insecure) {
        this.insecure = insecure;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(final int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(final int retryDelay) {
        this.retryDelay = retryDelay;
    }

    public CompressionMethod getCompressionMethod() {
        return compressionMethod;
    }

    public void setCompressionMethod(final CompressionMethod compressionMethod) {
        this.compressionMethod = compressionMethod;
    }

    @Override
    protected void startAppender() throws IOException {
        if (url == null) {
            url = "http://" + getGraylogHost() + ":" + getGraylogPort() + "/gelf";
        }

        if (insecure) {
            try {
                final SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new NoopX509TrustManager()},
                    new SecureRandom());
                sslSocketFactory = sslContext.getSocketFactory();
            } catch (final NoSuchAlgorithmException | KeyManagementException e) {
                throw new IOException("Failed to create SSLContext", e);
            }
        }
    }

    @Override
    protected void appendMessage(final byte[] messageToSend) throws IOException {
        final byte[] compressedMessage = compressionMethod.compress(messageToSend);

        int retries = maxRetries;
        do {
            try {
                sendMessage(compressedMessage);
                return;
            } catch (final IOException e) {
                if (retries > 0) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (final InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while waiting for retry",
                            interruptedException);
                    }
                } else {
                    throw e;
                }
            }
        } while (retries-- > 0);
    }

    private void sendMessage(final byte[] message) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        if (sslSocketFactory != null && connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
        }

        switch (compressionMethod) {
            case GZIP:
                connection.setRequestProperty("Content-Encoding", "gzip");
                break;
            case ZLIB:
                connection.setRequestProperty("Content-Encoding", "deflate");
                break;
            default:
                break;
        }

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(message);
        }

        final int responseCode = connection.getResponseCode();
        if (responseCode >= HttpURLConnection.HTTP_INTERNAL_ERROR) {
            throw new IOException("Server error: " + responseCode);
        } else if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            addError("Client error: " + responseCode + " for URL: " + url);
        }
    }

    @Override
    protected void close() {
        // No explicit close needed for HttpURLConnection as it is per-request
    }

}
