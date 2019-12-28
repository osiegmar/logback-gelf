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

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.LineReader;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

public class GelfEncoderTest {

    private static final String LOGGER_NAME = GelfEncoderTest.class.getCanonicalName();

    private final GelfEncoder encoder = new GelfEncoder();

    @BeforeEach
    public void before() {
        encoder.setContext(new LoggerContext());
        encoder.setOriginHost("localhost");
    }

    @Test
    public void simple() throws IOException {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = encodeToStr(simpleLoggingEvent(logger, null));

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);

        final LineReader msg =
            new LineReader(new StringReader(jsonNode.get("full_message").textValue()));
        assertEquals("message 1", msg.readLine());
    }

    @Test
    public void newline() {
        encoder.setAppendNewline(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = encodeToStr(simpleLoggingEvent(logger, null));

        assertTrue(logMsg.endsWith(System.lineSeparator()));
    }

    @Test
    public void nestedExceptionShouldNotFail() {
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        assertTimeout(ofMillis(400), () -> {
            final String logMsg = encodeToStr(simpleLoggingEvent(logger,
                new IOException(new IOException(new IOException()))));

            assertNotNull(logMsg);
        });
    }

    public static LoggingEvent simpleLoggingEvent(final Logger logger, final Throwable e) {
        return new LoggingEvent(
            LOGGER_NAME,
            logger,
            Level.DEBUG,
            "message {}",
            e,
            new Object[]{1});
    }

    private static void coreValidation(final JsonNode jsonNode) {
        assertEquals("1.1", jsonNode.get("version").textValue());
        assertEquals("localhost", jsonNode.get("host").textValue());
        assertEquals("message 1", jsonNode.get("short_message").textValue());
        assertEquals(7, jsonNode.get("level").intValue());
    }

    public static void basicValidation(final JsonNode jsonNode) {
        coreValidation(jsonNode);
        assertNotNull(jsonNode.get("_thread_name").textValue());
        assertEquals(LOGGER_NAME, jsonNode.get("_logger_name").textValue());
    }

    @Test
    public void exception() throws IOException {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IllegalArgumentException("Example Exception");
        } catch (final IllegalArgumentException e) {
            logMsg = encodeToStr(new LoggingEvent(
                LOGGER_NAME,
                logger,
                Level.DEBUG,
                "message {}",
                e,
                new Object[]{1}));
        }

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);

        final LineReader msg =
            new LineReader(new StringReader(jsonNode.get("full_message").textValue()));

        assertEquals("message 1", msg.readLine());
        assertEquals("java.lang.IllegalArgumentException: Example Exception", msg.readLine());
        final String line = msg.readLine();
        assertTrue(line.matches("^\tat de.siegmar.logbackgelf.GelfEncoderTest.exception"
            + "\\(GelfEncoderTest.java:\\d+\\)$"), "Unexpected line: " + line);
    }

    @Test
    public void complex() throws IOException {
        encoder.setIncludeRawMessage(true);
        encoder.setIncludeLevelName(true);
        encoder.addStaticField("foo:bar");
        encoder.setIncludeCallerData(true);
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        event.setMDCPropertyMap(ImmutableMap.of("mdc_key", "mdc_value"));

        final String logMsg = encodeToStr(event);

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);
        assertEquals("DEBUG", jsonNode.get("_level_name").textValue());
        assertEquals("bar", jsonNode.get("_foo").textValue());
        assertEquals("mdc_value", jsonNode.get("_mdc_key").textValue());
        assertEquals("message {}", jsonNode.get("_raw_message").textValue());
        assertNull(jsonNode.get("_exception"));
    }

    @Test
    public void customLevelNameKey() throws IOException {
        encoder.setIncludeLevelName(true);
        encoder.setLevelNameKey("Severity");
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        final String logMsg = encodeToStr(event);

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);
        assertEquals("DEBUG", jsonNode.get("_Severity").textValue());
        assertNull(jsonNode.get("_exception"));
    }

    @Test
    public void customLoggerNameKey() throws IOException {
        encoder.setLoggerNameKey("Logger");
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        final String logMsg = encodeToStr(event);

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        coreValidation(jsonNode);
        assertNotNull(jsonNode.get("_thread_name").textValue());
        assertEquals(LOGGER_NAME, jsonNode.get("_Logger").textValue());
        assertNull(jsonNode.get("_exception"));
    }

    @Test
    public void customThreadNameKey() throws IOException {
        encoder.setThreadNameKey("Thread");
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        final String logMsg = encodeToStr(event);

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        coreValidation(jsonNode);
        assertNotNull(jsonNode.get("_Thread").textValue());
        assertEquals(LOGGER_NAME, jsonNode.get("_logger_name").textValue());
        assertNull(jsonNode.get("_exception"));
    }

    @Test
    public void rootExceptionTurnedOff() throws IOException {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IOException("Example Exception");
        } catch (final IOException e) {
            logMsg = encodeToStr(simpleLoggingEvent(logger, e));
        }

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);

        assertFalse(jsonNode.has("_exception"));
    }

    @Test
    public void noRootException() throws IOException {
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = encodeToStr(simpleLoggingEvent(logger, null));

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);

        assertFalse(jsonNode.has("_exception"));
    }

    @Test
    public void rootExceptionWithoutCause() throws IOException {
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IOException("Example Exception");
        } catch (final IOException e) {
            logMsg = encodeToStr(simpleLoggingEvent(logger, e));
        }

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);

        assertEquals("java.io.IOException", jsonNode.get("_root_cause_class_name").textValue());
        assertEquals("Example Exception", jsonNode.get("_root_cause_message").textValue());
    }

    @Test
    public void rootExceptionWithCause() throws IOException {
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IOException("Example Exception",
                new IllegalStateException("Example Exception 2"));
        } catch (final IOException e) {
            logMsg = encodeToStr(simpleLoggingEvent(logger, e));
        }

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);

        assertEquals("java.lang.IllegalStateException",
            jsonNode.get("_root_cause_class_name").textValue());

        assertEquals("Example Exception 2",
            jsonNode.get("_root_cause_message").textValue());
    }

    @Test
    public void numericValueAsNumber() throws IOException {
        encoder.setNumbersAsString(false);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        event.setMDCPropertyMap(ImmutableMap.of("http_status", "200"));

        final String logMsg = encodeToStr(event);

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);
        final JsonNode httpStatus = jsonNode.get("_http_status");
        assertEquals(200, httpStatus.asDouble());
        assertTrue(httpStatus.isNumber());
    }

    @Test
    public void numericValueAsString() throws IOException {
        encoder.setNumbersAsString(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        event.setMDCPropertyMap(ImmutableMap.of("http_status", "200"));

        final String logMsg = encodeToStr(event);

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);
        final JsonNode httpStatus = jsonNode.get("_http_status");
        assertEquals("200", httpStatus.asText());
        assertFalse(httpStatus.isNumber());
    }

    @Test
    public void singleMarker() throws IOException {
        encoder.setLoggerNameKey("Logger");
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);
        event.setMarker(MarkerFactory.getMarker("SINGLE"));

        final String logMsg = encodeToStr(event);

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        coreValidation(jsonNode);
        assertEquals("SINGLE", jsonNode.get("_marker").textValue());
    }

    @Test
    public void multipleMarker() throws IOException {
        encoder.setLoggerNameKey("Logger");
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);
        final Marker marker = MarkerFactory.getMarker("FIRST");
        marker.add(MarkerFactory.getMarker("SECOND"));
        event.setMarker(marker);

        final String logMsg = encodeToStr(event);

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        coreValidation(jsonNode);
        assertEquals("FIRST, SECOND", jsonNode.get("_marker").textValue());
    }

    private String encodeToStr(final LoggingEvent event) {
        return new String(encoder.encode(event), StandardCharsets.UTF_8);
    }

}
