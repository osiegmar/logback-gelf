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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
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

    private UdpServerRunnable server;

    @BeforeEach
    public void before() throws IOException, InterruptedException {
        server = new UdpServerRunnable();
        final Thread serverThread = new Thread(server);
        serverThread.start();
    }

    @Test
    public void simple() throws IOException {
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
    public void compression() throws IOException {
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
        gelfAppender.setGraylogPort(server.getPort());
        gelfAppender.setUseCompression(useCompression);
        gelfAppender.start();
        return gelfAppender;
    }

    private JsonNode receiveMessage() throws IOException {
        return new ObjectMapper().readTree(server.getReceivedData());
    }

    private JsonNode receiveCompressedMessage() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(bos);

        inflaterOutputStream.write(server.getReceivedData());
        inflaterOutputStream.close();

        return new ObjectMapper().readTree(bos.toByteArray());
    }

    private void stopLogger(final Logger logger) {
        final GelfUdpAppender gelfAppender = (GelfUdpAppender) logger.getAppender("GELF");
        gelfAppender.stop();
    }

    private static final class UdpServerRunnable implements Runnable {

        private final DatagramSocket server;
        private final Semaphore semaphore = new Semaphore(1);
        private byte[] receivedData;

        UdpServerRunnable() throws IOException, InterruptedException {
            server = new DatagramSocket(0);
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
            final byte[] receiveData = new byte[1024];
            final DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
            try {
                server.receive(packet);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            } finally {
                server.close();
            }
            receivedData = Arrays.copyOf(packet.getData(), packet.getLength());
            semaphore.release();
        }

    }

}
