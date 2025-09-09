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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class GelfUdpAppenderTest {

    private static final String LOGGER_NAME = GelfUdpAppenderTest.class.getCanonicalName();

    private final UdpServer server;

    GelfUdpAppenderTest() throws SocketException {
        server = new UdpServer();
    }

    @AfterEach
    void after() {
        server.close();
    }

    @Timeout(3)
    @ParameterizedTest
    @EnumSource(CompressionMethod.class)
    void simple(final CompressionMethod method) throws ExecutionException, InterruptedException, TimeoutException {
        final Logger logger = setupLogger(method);

        logger.error("Test message");

        stopLogger(logger);

        final String json = awaitMessage(method);
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

    private Logger setupLogger(final CompressionMethod compressionMethod) {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        final GelfEncoder gelfEncoder = new GelfEncoder();
        gelfEncoder.setContext(lc);
        gelfEncoder.setOriginHost("localhost");
        gelfEncoder.start();

        final Logger logger = (Logger) LoggerFactory.getLogger(LOGGER_NAME);
        logger.addAppender(buildAppender(compressionMethod, lc, gelfEncoder));
        logger.setAdditive(false);

        return logger;
    }

    private GelfUdpAppender buildAppender(final CompressionMethod compressionMethod,
                                          final LoggerContext lc,
                                          final GelfEncoder gelfEncoder) {
        final GelfUdpAppender gelfAppender = new GelfUdpAppender();
        gelfAppender.setContext(lc);
        gelfAppender.setName("GELF");
        gelfAppender.setEncoder(gelfEncoder);
        gelfAppender.setGraylogHost("localhost");
        gelfAppender.setGraylogPort(server.getPort());
        gelfAppender.setCompressionMethod(compressionMethod);
        gelfAppender.start();
        return gelfAppender;
    }

    private void stopLogger(final Logger logger) {
        final GelfUdpAppender gelfAppender = (GelfUdpAppender) logger.getAppender("GELF");
        gelfAppender.stop();
    }

    private String awaitMessage(final CompressionMethod compressionMethod)
        throws ExecutionException, InterruptedException, TimeoutException {

        final byte[] receivedData = server.receiveMessage().get(5, TimeUnit.SECONDS);
        return new String(Decompressor.decompress(receivedData, compressionMethod), StandardCharsets.UTF_8);
    }

    private static final class UdpServer implements Closeable {

        private final DatagramSocket socket;
        private final Future<byte[]> receivedMessage;

        UdpServer() throws SocketException {
            socket = new DatagramSocket(0);

            receivedMessage = Executors.newSingleThreadExecutor()
                .submit(this::receive);
        }

        int getPort() {
            return socket.getLocalPort();
        }

        Future<byte[]> receiveMessage() {
            return receivedMessage;
        }

        private byte[] receive() {
            final byte[] receiveData = new byte[1024];
            final DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
            try {
                socket.receive(packet);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
            return Arrays.copyOf(packet.getData(), packet.getLength());
        }

        @Override
        public void close() {
            socket.close();
        }

    }

    private static final class Decompressor {

        @SuppressWarnings("PMD.ExhaustiveSwitchHasDefault")
        static byte[] decompress(final byte[] bytesIn, final CompressionMethod compressionMethod) {
            switch (compressionMethod) {
                case NONE:
                    return bytesIn;
                case GZIP:
                    return gzipDecompress(bytesIn);
                case ZLIB:
                    return zlibDecompress(bytesIn);
                default:
                    throw new IllegalStateException();
            }
        }

        private static byte[] zlibDecompress(final byte[] bytesIn) {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final OutputStream inflaterOutputStream = new InflaterOutputStream(bos);

            try {
                inflaterOutputStream.write(bytesIn);
                inflaterOutputStream.close();

                return bos.toByteArray();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static byte[] gzipDecompress(final byte[] bytesIn) {
            try {
                return new GZIPInputStream(new ByteArrayInputStream(bytesIn)).readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

    }

}
