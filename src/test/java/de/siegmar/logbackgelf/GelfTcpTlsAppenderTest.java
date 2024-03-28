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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

class GelfTcpTlsAppenderTest {

    private static final String LOGGER_NAME = GelfTcpTlsAppenderTest.class.getCanonicalName();

    private final TlsServer server;

    GelfTcpTlsAppenderTest() throws IOException {
        server = new TlsServer();
    }

    @Test
    void defaultValues() {
        final GelfTcpTlsAppender appender = new GelfTcpTlsAppender();
        assertThat(appender.isInsecure()).isFalse();
    }

    @Test
    void simple() throws ExecutionException, InterruptedException, TimeoutException {
        final Logger logger = setupLogger();

        logger.error("Test message");

        stopLogger(logger);

        final String json = awaitMessage();
        assertThatJson(json).and(
            j -> j.node("version").isString().isEqualTo("1.1"),
            j -> j.node("host").isEqualTo("localhost"),
            j -> j.node("short_message").isEqualTo("Test message"),
            j -> j.node("timestamp").isNumber(),
            j -> j.node("level").isEqualTo(3),
            j -> j.node("_thread_name").isNotNull(),
            j -> j.node("_logger_name").isEqualTo(LOGGER_NAME)
        );
    }

    private Logger setupLogger() {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        final GelfEncoder gelfEncoder = new GelfEncoder();
        gelfEncoder.setContext(lc);
        gelfEncoder.setOriginHost("localhost");
        gelfEncoder.start();

        final Logger logger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
        logger.addAppender(buildAppender(lc, gelfEncoder));
        logger.setAdditive(false);

        return logger;
    }

    private GelfTcpTlsAppender buildAppender(final LoggerContext lc, final GelfEncoder encoder) {
        final GelfTcpTlsAppender gelfAppender = new GelfTcpTlsAppender();
        gelfAppender.setContext(lc);
        gelfAppender.setName("GELF");
        gelfAppender.setEncoder(encoder);
        gelfAppender.setGraylogHost("localhost");
        gelfAppender.setGraylogPort(server.getPort());
        gelfAppender.setInsecure(true);
        gelfAppender.start();
        return gelfAppender;
    }

    private void stopLogger(final Logger logger) {
        final GelfTcpTlsAppender gelfAppender = (GelfTcpTlsAppender) logger.getAppender("GELF");
        gelfAppender.stop();
    }

    private String awaitMessage() throws ExecutionException, InterruptedException, TimeoutException {
        final byte[] data = server.receiveMessage().get(5, TimeUnit.SECONDS);
        if (data[data.length - 1] != 0) {
            throw new IllegalStateException("Data stream is not terminated by 0");
        }

        return new String(data, 0, data.length - 1, StandardCharsets.UTF_8);
    }

    private static final class TlsServer implements Closeable {

        private final SSLServerSocket socket;
        private final Future<byte[]> receivedMessage;

        TlsServer() {
            socket = initSocket();

            receivedMessage = Executors.newSingleThreadExecutor()
                .submit(this::receive);
        }

        private SSLServerSocket initSocket() {
            final var socketFactory = getSSLServerSocketFactory(getFromPath());
            try {
                return (SSLServerSocket) socketFactory.createServerSocket(0);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }

        public static SSLServerSocketFactory getSSLServerSocketFactory(final KeyStore trustKey) {
            try {
                final var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(trustKey, "secret".toCharArray());

                final var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustKey);

                final var context = SSLContext.getInstance("TLS");
                context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                return context.getServerSocketFactory();
            } catch (final UnrecoverableKeyException | NoSuchAlgorithmException | KeyManagementException
                           | KeyStoreException e) {
                throw new IllegalStateException(e);
            }
        }

        public static KeyStore getFromPath() {
            try (var keyFile = Files.newInputStream(Path.of("src/test/resources/mySrvKeystore"))) {
                final var keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(keyFile, "secret".toCharArray());
                return keyStore;
            } catch (final IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        int getPort() {
            return socket.getLocalPort();
        }

        Future<byte[]> receiveMessage() {
            return receivedMessage;
        }

        private byte[] receive() throws IOException {
            try (Socket socket = this.socket.accept()) {
                try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
                    return in.readAllBytes();
                }
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }

    }

}
