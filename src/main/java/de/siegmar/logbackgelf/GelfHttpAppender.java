/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2024 Oliver Siegmar
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import de.siegmar.logbackgelf.compressor.Compressor;

@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class GelfHttpAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    private static final int DEFAULT_CONNECT_TIMEOUT = 15_000;
    private static final int DEFAULT_REQUEST_TIMEOUT = 5_000;
    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final int DEFAULT_RETRY_DELAY = 3_000;

    /**
     * The URI to send messages to.
     */
    private URI uri;

    /**
     * If {@code true}, skip the TLS certificate validation.
     */
    private boolean insecure;

    /**
     * Maximum time (in milliseconds) to wait for establishing a connection. A value of 0 disables
     * the connect timeout. Default: {@value DEFAULT_CONNECT_TIMEOUT} milliseconds.
     */
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    private int requestTimeout = DEFAULT_REQUEST_TIMEOUT;

    /**
     * Number of retries. A value of 0 disables retry attempts. Default: {@value DEFAULT_MAX_RETRIES}.
     */
    private int maxRetries = DEFAULT_MAX_RETRIES;

    /**
     * Time (in milliseconds) between retry attempts. Ignored if maxRetries is 0.
     * Default: {@value DEFAULT_RETRY_DELAY} milliseconds.
     */
    private int retryDelay = DEFAULT_RETRY_DELAY;

    /**
     * Compression method used (NONE, GZIP or ZLIB). Default: GZIP.
     */
    private CompressionMethod compressionMethod = CompressionMethod.GZIP;

    /**
     * The HTTP client to use for sending messages.
     */
    private HttpClient httpClient;

    /**
     * The encoder to use for encoding log messages.
     */
    private GelfEncoder encoder;

    private Compressor compressor;

    public URI getUri() {
        return uri;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    public boolean isInsecure() {
        return insecure;
    }

    public void setInsecure(final boolean insecure) {
        this.insecure = insecure;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(final int requestTimeout) {
        this.requestTimeout = requestTimeout;
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

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public GelfEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(final GelfEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void start() {
        if (uri == null) {
            addError("URI is required");
            return;
        }

        if (encoder == null) {
            encoder = new GelfEncoder();
            encoder.setContext(getContext());
            encoder.start();
        } else if (encoder.isAppendNewline()) {
            addError("Newline separator must not be enabled in layout");
            return;
        }

        if (httpClient == null) {
            try {
                httpClient = buildHttpClient();
            } catch (final NoSuchAlgorithmException | KeyManagementException e) {
                addError("Failed to create HttpClient", e);
                return;
            }
        }

        compressor = compressionMethod.getCompressor();

        super.start();
    }

    private HttpClient buildHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        final HttpClient.Builder builder = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofMillis(connectTimeout));

        if (insecure) {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new NoopX509TrustManager()}, new SecureRandom());
            builder.sslContext(sslContext);
        }

        return builder.build();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    protected void append(final ILoggingEvent event) {
        try {
            final HttpRequest request = buildRequest(buildPackage(event));
            RetryUtil.retry(() -> sendRequest(request), this::isStarted, maxRetries, retryDelay);
        } catch (final Exception e) {
            addError(String.format("Error sending message via %s", getUri()), e);
        }
    }

    private byte[] buildPackage(final ILoggingEvent event) {
        return compressor.compress(encoder.encode(event));
    }

    private HttpRequest buildRequest(final byte[] data) {
        final HttpRequest.Builder reqB = HttpRequest.newBuilder(uri)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(requestTimeout));

        contentEncoding()
            .ifPresent(encoding -> reqB.header("Content-Encoding", encoding));

        return reqB
            .POST(HttpRequest.BodyPublishers.ofByteArray(data))
            .build();
    }

    private Optional<String> contentEncoding() {
        switch (compressionMethod) {
            case GZIP:
                return Optional.of("gzip");
            case ZLIB:
                return Optional.of("deflate");
            case NONE:
                return Optional.empty();
            default:
                throw new IllegalStateException("Unknown compression method: " + compressionMethod);
        }
    }

    /**
     * Send request to Graylog server.
     *
     * @param request HTTP request to send.
     * @return status code of the response.
     */
    private int sendRequest(final HttpRequest request) throws IOException, InterruptedException {
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        final int statusCode = response.statusCode();

        if (statusCode >= HTTP_INTERNAL_SERVER_ERROR) {
            // Throw exception for server errors (retry)
            throw new IllegalStateException(String.format("Error sending message via %s. Code: %s; Message: %s",
                getUri(), response.statusCode(), response.body()));
        }

        if (statusCode >= HTTP_BAD_REQUEST) {
            // Don't throw exception for client errors (no retry)
            addError(String.format("Error sending message via %s. Code: %s; Message: %s",
                getUri(), response.statusCode(), response.body()));
        }

        return statusCode;
    }

    @Override
    public void stop() {
        encoder.stop();

        super.stop();
    }

}
