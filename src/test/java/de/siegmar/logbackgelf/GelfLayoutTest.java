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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.LineReader;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

public class GelfLayoutTest {

    private static final String LOGGER_NAME = GelfLayoutTest.class.getCanonicalName();

    private final GelfLayout layout = new GelfLayout();

    @Before
    public void before() {
        layout.setContext(new LoggerContext());
        layout.setOriginHost("localhost");
    }

    @Test
    public void simple() throws IOException {
        layout.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = layout.doLayout(simpleLoggingEvent(logger, null));

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);

        final LineReader msg =
            new LineReader(new StringReader(jsonNode.get("full_message").textValue()));
        assertEquals("message 1", msg.readLine());
    }

    @Test
    public void newline() {
        layout.setAppendNewline(true);
        layout.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = layout.doLayout(simpleLoggingEvent(logger, null));

        assertTrue(logMsg.endsWith(System.lineSeparator()));
    }

    @Test(timeout = 400L)
    public void nestedExceptionShouldNotFail() {
        layout.setIncludeRootCauseData(true);
        layout.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = layout.doLayout(simpleLoggingEvent(logger,
            new IOException(new IOException(new IOException()))));

        assertNotNull(logMsg);
    }

    private LoggingEvent simpleLoggingEvent(final Logger logger, final Throwable e) {
        return new LoggingEvent(
            LOGGER_NAME,
            logger,
            Level.DEBUG,
            "message {}",
            e,
            new Object[]{1});
    }

    private void basicValidation(final JsonNode jsonNode) {
        assertEquals("1.1", jsonNode.get("version").textValue());
        assertEquals("localhost", jsonNode.get("host").textValue());
        assertEquals("message 1", jsonNode.get("short_message").textValue());
        assertEquals(7, jsonNode.get("level").intValue());
        assertNotNull(jsonNode.get("_thread_name").textValue());
        assertEquals(LOGGER_NAME, jsonNode.get("_logger_name").textValue());
    }

    @Test
    public void exception() throws IOException {
        layout.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IllegalArgumentException("Example Exception");
        } catch (final IllegalArgumentException e) {
            logMsg = layout.doLayout(new LoggingEvent(
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
        assertTrue("Unexpected line: " + line, line.matches(
            "^\tat de.siegmar.logbackgelf.GelfLayoutTest.exception"
                + "\\(GelfLayoutTest.java:\\d+\\)$"));
    }

    @Test
    public void complex() throws IOException {
        layout.setIncludeRawMessage(true);
        layout.setIncludeLevelName(true);
        layout.addStaticField("foo:bar");
        layout.setIncludeCallerData(true);
        layout.setIncludeRootCauseData(true);
        layout.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        event.setMDCPropertyMap(ImmutableMap.of("mdc_key", "mdc_value"));

        final String logMsg = layout.doLayout(event);

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
    public void complexStaticFieldArray() throws IOException {
        layout.setIncludeRawMessage(true);
        layout.setIncludeLevelName(true);
        layout.addStaticField("foo:[bar,barr]");
        layout.setIncludeCallerData(true);
        layout.setIncludeRootCauseData(true);
        layout.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        event.setMDCPropertyMap(ImmutableMap.of("mdc_key", "mdc_value"));

        final String logMsg = layout.doLayout(event);

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);
        assertEquals("DEBUG", jsonNode.get("_level_name").textValue());
        assertEquals("bar", jsonNode.get("_foo").get(0).textValue());
        assertEquals("barr", jsonNode.get("_foo").get(1).textValue());
        assertEquals("mdc_value", jsonNode.get("_mdc_key").textValue());
        assertEquals("message {}", jsonNode.get("_raw_message").textValue());
        assertNull(jsonNode.get("_exception"));
    }

    @Test
    public void rootExceptionTurnedOff() throws IOException {
        layout.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IOException("Example Exception");
        } catch (final IOException e) {
            logMsg = layout.doLayout(simpleLoggingEvent(logger, e));
        }

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);

        assertFalse(jsonNode.has("_exception"));
    }

    @Test
    public void noRootException() throws IOException {
        layout.setIncludeRootCauseData(true);
        layout.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = layout.doLayout(simpleLoggingEvent(logger, null));

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);

        assertFalse(jsonNode.has("_exception"));
    }

    @Test
    public void rootExceptionWithoutCause() throws IOException {
        layout.setIncludeRootCauseData(true);
        layout.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IOException("Example Exception");
        } catch (final IOException e) {
            logMsg = layout.doLayout(simpleLoggingEvent(logger, e));
        }

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);

        assertEquals("java.io.IOException", jsonNode.get("_root_cause_class_name").textValue());
        assertEquals("Example Exception", jsonNode.get("_root_cause_message").textValue());
    }

    @Test
    public void rootExceptionWithCause() throws IOException {
        layout.setIncludeRootCauseData(true);
        layout.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IOException("Example Exception",
                new IllegalStateException("Example Exception 2"));
        } catch (final IOException e) {
            logMsg = layout.doLayout(simpleLoggingEvent(logger, e));
        }

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);

        assertEquals("java.lang.IllegalStateException",
            jsonNode.get("_root_cause_class_name").textValue());

        assertEquals("Example Exception 2",
            jsonNode.get("_root_cause_message").textValue());
    }

}
