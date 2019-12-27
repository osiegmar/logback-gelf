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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class GelfTcpTlsAppenderTest {

    private static final String LOGGER_NAME = GelfTcpTlsAppenderTest.class.getCanonicalName();

    private TcpServerRunnable server;

    public GelfTcpTlsAppenderTest() {
        final String mySrvKeystore =
            GelfTcpTlsAppenderTest.class.getResource("/mySrvKeystore").getFile();
        System.setProperty("javax.net.ssl.keyStore", mySrvKeystore);
        System.setProperty("javax.net.ssl.keyStorePassword", "secret");
    }

    @BeforeEach
    public void before() throws IOException, InterruptedException {
        server = new TcpServerRunnable();
        final Thread serverThread = new Thread(server);
        serverThread.start();
    }

    @Test
    public void simple() throws IOException {
        final Logger logger = setupLogger();

        logger.error("Test message");

        stopLogger(logger);

        final JsonNode jsonNode = receiveMessage();
        assertEquals("1.1", jsonNode.get("version").textValue());
        assertEquals("localhost", jsonNode.get("host").textValue());
        assertEquals("Test message", jsonNode.get("short_message").textValue());
        assertTrue(jsonNode.get("timestamp").isNumber());
        assertEquals(3, jsonNode.get("level").intValue());
        assertNotNull(jsonNode.get("_thread_name").textValue());
        assertEquals(LOGGER_NAME, jsonNode.get("_logger_name").textValue());
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

    private JsonNode receiveMessage() throws IOException {
        return new ObjectMapper().readTree(server.getReceivedData());
    }

    private void stopLogger(final Logger logger) {
        final GelfTcpTlsAppender gelfAppender = (GelfTcpTlsAppender) logger.getAppender("GELF");
        gelfAppender.stop();
    }

    private static final class TcpServerRunnable implements Runnable {

        private final SSLServerSocket server;
        private final Semaphore semaphore = new Semaphore(1);
        private byte[] receivedData;

        TcpServerRunnable() throws IOException, InterruptedException {
            final SSLServerSocketFactory sslserversocketfactory =
                (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            server = (SSLServerSocket) sslserversocketfactory.createServerSocket(0);
            semaphore.acquire();
        }

        int getPort() {
            return server.getLocalPort();
        }

        byte[] getReceivedData() {
            try {
                if (!semaphore.tryAcquire(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Couldn't acquire semaphore!");
                }
            } catch (final InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return receivedData;
        }

        @Override
        public void run() {
            try (Socket socket = server.accept()) {
                try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
                    receivedData = ByteStreams.toByteArray(in);
                }
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }

            try {
                server.close();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }

            semaphore.release();
        }

    }

}
