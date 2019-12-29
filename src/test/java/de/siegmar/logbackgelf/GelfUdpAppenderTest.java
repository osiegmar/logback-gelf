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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.InflaterOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class GelfUdpAppenderTest {

    private static final String LOGGER_NAME = GelfUdpAppenderTest.class.getCanonicalName();

    private int port;
    private Future<byte[]> future;

    @BeforeEach
    public void before() throws IOException {
        final UdpServer server = new UdpServer();
        port = server.getPort();
        future = Executors.newSingleThreadExecutor().submit(server);
    }

    @Test
    public void simple() {
        final Logger logger = setupLogger(false);

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

    @Test
    public void compression() {
        final Logger logger = setupLogger(true);

        logger.error("Test message");

        stopLogger(logger);

        final JsonNode jsonNode = receiveCompressedMessage();
        assertEquals("1.1", jsonNode.get("version").textValue());
        assertEquals("localhost", jsonNode.get("host").textValue());
        assertEquals("Test message", jsonNode.get("short_message").textValue());
        assertTrue(jsonNode.get("timestamp").isNumber());
        assertEquals(3, jsonNode.get("level").intValue());
        assertNotNull(jsonNode.get("_thread_name").textValue());
        assertEquals(LOGGER_NAME, jsonNode.get("_logger_name").textValue());
    }

    private Logger setupLogger(final boolean useCompression) {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        final GelfEncoder gelfEncoder = new GelfEncoder();
        gelfEncoder.setContext(lc);
        gelfEncoder.setOriginHost("localhost");
        gelfEncoder.start();

        final Logger logger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
        logger.addAppender(buildAppender(useCompression, lc, gelfEncoder));
        logger.setAdditive(false);

        return logger;
    }

    private GelfUdpAppender buildAppender(final boolean useCompression, final LoggerContext lc,
                                          final GelfEncoder gelfEncoder) {
        final GelfUdpAppender gelfAppender = new GelfUdpAppender();
        gelfAppender.setContext(lc);
        gelfAppender.setName("GELF");
        gelfAppender.setEncoder(gelfEncoder);
        gelfAppender.setGraylogHost("localhost");
        gelfAppender.setGraylogPort(port);
        gelfAppender.setUseCompression(useCompression);
        gelfAppender.start();
        return gelfAppender;
    }

    private JsonNode receiveMessage() {
        try {
            return new ObjectMapper().readTree(receive());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JsonNode receiveCompressedMessage() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(bos);

        try {
            inflaterOutputStream.write(receive());
            inflaterOutputStream.close();

            return new ObjectMapper().readTree(bos.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void stopLogger(final Logger logger) {
        final GelfUdpAppender gelfAppender = (GelfUdpAppender) logger.getAppender("GELF");
        gelfAppender.stop();
    }

    private byte[] receive() {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class UdpServer implements Callable<byte[]> {

        private final DatagramSocket server;

        UdpServer() throws IOException {
            server = new DatagramSocket(0);
        }

        int getPort() {
            return server.getLocalPort();
        }

        @Override
        public byte[] call() throws Exception {
            final byte[] receiveData = new byte[1024];
            final DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
            try {
                server.receive(packet);
            } finally {
                server.close();
            }

            return Arrays.copyOf(packet.getData(), packet.getLength());
        }
    }

}
