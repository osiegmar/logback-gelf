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

import static java.util.Map.entry;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "PMD.AvoidDuplicateLiterals"})
class GelfEncoderTest {

    private static final String LOGGER_NAME = GelfEncoderTest.class.getCanonicalName();

    private final GelfEncoder encoder = new GelfEncoder();

    @BeforeEach
    void before() {
        encoder.setContext(new LoggerContext());
        encoder.setOriginHost("localhost");
    }

    @Test
    void simple() {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        assertThatJson(encodeToStr(simpleLoggingEvent(logger, null)))
            .node("full_message").isEqualTo("message 1\\n");
    }

    @Test
    void newline() {
        encoder.setAppendNewline(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = encodeToStr(simpleLoggingEvent(logger, null));

        assertThat(logMsg).endsWith(System.lineSeparator());
    }

    @Test
    void nestedExceptionShouldNotFail() {
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = encodeToStr(simpleLoggingEvent(logger,
            new IOException(new IOException(new IOException()))));

        assertThat(logMsg).isNotNull();
    }

    static LoggingEvent simpleLoggingEvent(final Logger logger, final Throwable e) {
        return new LoggingEvent(
            LOGGER_NAME,
            logger,
            Level.DEBUG,
            "message {}",
            e,
            new Object[]{1});
    }

    private static void coreValidation(final String json) {
        assertThatJson(json).and(
            j -> j.node("version").asString().isEqualTo("1.1"),
            j -> j.node("host").isEqualTo("localhost"),
            j -> j.node("short_message").isEqualTo("message 1"),
            j -> j.node("level").isEqualTo(7)
        );
    }

    static void basicValidation(final String json) {
        coreValidation(json);
        assertThatJson(json).and(
            j -> j.node("_thread_name").isNotNull(),
            j -> j.node("_logger_name").isEqualTo(LOGGER_NAME)
        );
    }

    @Test
    void exception() {
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

        basicValidation(logMsg);

        assertThatJson(logMsg).node("full_message").asString()
            .startsWith("message 1\njava.lang.IllegalArgumentException: Example Exception\n");
    }

    @Test
    void keyValues() {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);
        event.addKeyValuePair(new KeyValuePair("key1", "value"));
        event.addKeyValuePair(new KeyValuePair("key2", 123));
        event.addKeyValuePair(new KeyValuePair("key3", true));

        final String logMsg = encodeToStr(event);

        assertThatJson(logMsg).and(
            j -> j.node("_key1").isEqualTo("value"),
            j -> j.node("_key2").isEqualTo(123),
            j -> j.node("_key3").isString().isEqualTo("true")
        );
    }

    @Test
    void complex() {
        encoder.setIncludeRawMessage(true);
        encoder.setIncludeLevelName(true);
        encoder.addStaticField("foo:bar");
        encoder.addStaticField("bar", "baz");
        encoder.setIncludeCallerData(true);
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        event.setMDCPropertyMap(Map.of("mdc_key", "mdc_value"));

        final String logMsg = encodeToStr(event);

        assertThatJson(logMsg).and(
            j -> j.node("_level_name").isEqualTo("DEBUG"),
            j -> j.node("_foo").isEqualTo("bar"),
            j -> j.node("_bar").isEqualTo("baz"),
            j -> j.node("_mdc_key").isEqualTo("mdc_value"),
            j -> j.node("_raw_message").isEqualTo("message {}"),
            j -> j.node("_exception").isAbsent()
        );
    }

    @Test
    void customLevelNameKey() {
        encoder.setIncludeLevelName(true);
        encoder.setLevelNameKey("Severity");
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        final String logMsg = encodeToStr(event);

        assertThatJson(logMsg).and(
            j -> j.node("_Severity").isEqualTo("DEBUG"),
            j -> j.node("_exception").isAbsent()
        );
    }

    @Test
    void customLoggerNameKey() {
        encoder.setLoggerNameKey("Logger");
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        final String logMsg = encodeToStr(event);

        assertThatJson(logMsg).and(
            j -> j.node("_thread_name").isNotNull(),
            j -> j.node("_Logger").isEqualTo(LOGGER_NAME),
            j -> j.node("_exception").isAbsent()
        );
    }

    @Test
    void customThreadNameKey() {
        encoder.setThreadNameKey("Thread");
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        final String logMsg = encodeToStr(event);

        assertThatJson(logMsg).and(
            j -> j.node("_Thread").isNotNull(),
            j -> j.node("_logger_name").isEqualTo(LOGGER_NAME),
            j -> j.node("_exception").isAbsent()
        );
    }

    @Test
    void rootExceptionTurnedOff() {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IOException("Example Exception");
        } catch (final IOException e) {
            logMsg = encodeToStr(simpleLoggingEvent(logger, e));
        }

        assertThatJson(logMsg).node("_exception").isAbsent();
    }

    @Test
    void noRootException() {
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = encodeToStr(simpleLoggingEvent(logger, null));

        assertThatJson(logMsg).node("_exception").isAbsent();
    }

    @Test
    void rootExceptionWithoutCause() {
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

        basicValidation(logMsg);
        assertThatJson(logMsg).and(
            j -> j.node("_root_cause_class_name").isEqualTo("java.io.IOException"),
            j -> j.node("_root_cause_message").isEqualTo("Example Exception")
        );
    }

    @Test
    void rootExceptionWithCause() {
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

        basicValidation(logMsg);
        assertThatJson(logMsg).and(
            j -> j.node("_root_cause_class_name").isEqualTo("java.lang.IllegalStateException"),
            j -> j.node("_root_cause_message").isEqualTo("Example Exception 2")
        );
    }

    @Test
    void numericValueAsNumber() {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        event.setMDCPropertyMap(Map.of("int", "200", "float", "0.00001"));

        final String logMsg = encodeToStr(event);

        basicValidation(logMsg);
        assertThatJson(logMsg).and(
            j -> j.node("_int").isNumber().isEqualTo("200"),
            j -> j.node("_float").isNumber().isEqualTo("0.00001")
        );
    }

    @Test
    void numericValueAsString() {
        encoder.setNumbersAsString(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        event.setMDCPropertyMap(Map.of("int", "200", "float", "0.00001"));

        final String logMsg = encodeToStr(event);

        basicValidation(logMsg);
        assertThatJson(logMsg).and(
            j -> j.node("_int").isString().isEqualTo("200"),
            j -> j.node("_float").isString().isEqualTo("0.00001")
        );
    }

    @ParameterizedTest
    @MethodSource("singleMarkerTestsArgumentsSource")
    void singleMarker(boolean soloMarker, String expectedMarkerLog) {
        encoder.setLoggerNameKey("Logger");
        encoder.setIncludeMarker(true);
        encoder.setSoloMarker(soloMarker);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);
        event.addMarker(MarkerFactory.getMarker("SINGLE"));

        final String logMsg = encodeToStr(event);

        coreValidation(logMsg);
        assertThatJson(logMsg).and(
            j -> j.node("_marker").isString().isEqualTo(expectedMarkerLog)
        );
    }


    private static Stream<Arguments> singleMarkerTestsArgumentsSource() {
        return Stream.of(
                Arguments.of(false, "[SINGLE]"),
                Arguments.of(true, "SINGLE")
                        );
    }

    @ParameterizedTest
    @MethodSource("multipleMarkerTestsArgumentsSource")
    void multipleMarker(boolean soloMarker, String expectedMarkerLog) {
        encoder.setLoggerNameKey("Logger");
        encoder.setIncludeMarker(true);
        encoder.setSoloMarker(soloMarker);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);
        final Marker marker = MarkerFactory.getMarker("FIRST");
        marker.add(MarkerFactory.getMarker("SECOND"));
        event.addMarker(marker);
        event.addMarker(MarkerFactory.getMarker("THIRD"));

        final String logMsg = encodeToStr(event);

        coreValidation(logMsg);
        assertThatJson(logMsg).and(
            j -> j.node("_marker").isString().isEqualTo(expectedMarkerLog)
        );
    }

    private static Stream<Arguments> multipleMarkerTestsArgumentsSource() {
        return Stream.of(
                Arguments.of(false, "[FIRST [ SECOND ], THIRD]"),
                Arguments.of(true, "FIRST")
                        );
    }

    private String encodeToStr(final LoggingEvent event) {
        return new String(encoder.encode(event), StandardCharsets.UTF_8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing colon", "id:value", "$key:value", "#key:value", "new$key:value"})
    void invalidStaticField(final String staticField) {
        encoder.addStaticField(staticField);
        assertThat(encoder.getStaticFields()).isEmpty();
    }

    @Test
    void rewriteStaticField() {
        encoder.addStaticField("test_id:value");
        encoder.addStaticField("test_id:new value");
        assertThat(encoder.getStaticFields())
            .containsExactly(entry("test_id", "value"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void originHostDefaultToLocalHostNameIfEmpty(final String configuredHostname) {
        encoder.setOriginHost(configuredHostname);
        encoder.start();

        assertThat(encoder.getOriginHost())
            .isNotBlank()
            .isNotEqualTo("unknown");
    }

    @ParameterizedTest
    @ValueSource(strings = { " \t ", "", "\n" })
    void blankShortMessage(final String message) {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = new LoggingEvent(LOGGER_NAME, logger, Level.DEBUG, message, null, new Object[]{1});
        final String logMsg = encodeToStr(event);

        assertThatJson(logMsg).node("short_message")
            .isEqualTo("Blank message replaced by logback-gelf");
    }

    @Test
    void defaultValues() {
        assertThat(encoder).satisfies(
            e -> assertThat(e.isIncludeRawMessage()).isFalse(),
            e -> assertThat(e.isIncludeMarker()).isFalse(),
            e -> assertThat(e.isIncludeMdcData()).isTrue(),
            e -> assertThat(e.isIncludeCallerData()).isFalse(),
            e -> assertThat(e.isIncludeRootCauseData()).isFalse(),
            e -> assertThat(e.isIncludeLevelName()).isFalse(),
            e -> assertThat(e.getOriginHost()).isEqualTo("localhost"),
            e -> assertThat(e.getLevelNameKey()).isEqualTo("level_name"),
            e -> assertThat(e.getLoggerNameKey()).isEqualTo("logger_name"),
            e -> assertThat(e.getThreadNameKey()).isEqualTo("thread_name"),
            e -> assertThat(e.isAppendNewline()).isFalse(),
            e -> assertThat(e.isNumbersAsString()).isFalse()
        );
    }

    @Test
    void addFieldMapper() {
        final GelfFieldMapper<Object> fieldMapper = (event, valueHandler) -> { };
        encoder.addFieldMapper(fieldMapper);
        assertThat(encoder.getFieldMappers()).containsExactly(fieldMapper);
    }

    @Test
    void hasDefaultPatternLayout() {
        encoder.start();

        assertThat(encoder.getFullMessageLayout()).isNotNull();
        assertThat(encoder.getShortMessageLayout()).isNotNull();
    }

}
