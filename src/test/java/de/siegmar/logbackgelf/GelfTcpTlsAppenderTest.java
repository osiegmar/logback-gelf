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

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

class GelfTcpTlsAppenderTest {

    private static final String LOGGER_NAME = GelfTcpTlsAppenderTest.class.getCanonicalName();

    private int port;
    private Future<byte[]> future;

    GelfTcpTlsAppenderTest() {
        final String mySrvKeystore =
            GelfTcpTlsAppenderTest.class.getResource("/mySrvKeystore").getFile();
        System.setProperty("javax.net.ssl.keyStore", mySrvKeystore);
        System.setProperty("javax.net.ssl.keyStorePassword", "secret");
    }

    @BeforeEach
    void before() throws IOException {
        final TcpServer server = new TcpServer();
        port = server.getPort();
        future = Executors.newSingleThreadExecutor().submit(server);
    }

    @Test
    void defaultValues() {
        final GelfTcpTlsAppender appender = new GelfTcpTlsAppender();
        assertThat(appender.isInsecure()).isFalse();
    }

    @Test
    void simple() {
        final Logger logger = setupLogger();

        logger.error("Test message");

        stopLogger(logger);

        final String json = receiveMessage();
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
        gelfAppender.setGraylogPort(port);
        gelfAppender.setInsecure(true);
        gelfAppender.start();
        return gelfAppender;
    }

    private String receiveMessage() {
        return new String(receive(), StandardCharsets.UTF_8);
    }

    private void stopLogger(final Logger logger) {
        final GelfTcpTlsAppender gelfAppender = (GelfTcpTlsAppender) logger.getAppender("GELF");
        gelfAppender.stop();
    }

    private byte[] receive() {
        try {
            final byte[] bytes = future.get(5, TimeUnit.SECONDS);
            if (bytes[bytes.length - 1] != 0) {
                throw new IllegalStateException("Data stream is not terminated by 0");
            }
            return Arrays.copyOf(bytes, bytes.length - 1);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class TcpServer implements Callable<byte[]> {

        private final SSLServerSocket server;

        TcpServer() throws IOException {
            final SSLServerSocketFactory sslserversocketfactory =
                (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            server = (SSLServerSocket) sslserversocketfactory.createServerSocket(0);
        }

        int getPort() {
            return server.getLocalPort();
        }

        @Override
        public byte[] call() throws Exception {
            final byte[] ret;

            try (server; Socket socket = server.accept()) {
                try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
                    ret = in.readAllBytes();
                }
            }

            return ret;
        }

    }

}
