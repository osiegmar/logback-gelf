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

import static de.siegmar.logbackgelf.GelfEncoderTest.basicValidation;
import static de.siegmar.logbackgelf.GelfEncoderTest.simpleLoggingEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import de.siegmar.logbackgelf.custom.CustomGelfEncoder;

public class CustomGelfEncoderTest {

    private static final String LOGGER_NAME = GelfEncoderTest.class.getCanonicalName();
    private static final String THREAD_NAME = "thread name";
    private static final long TIMESTAMP = 1577359700000L;

    private final CustomGelfEncoder encoder = new CustomGelfEncoder();

    @BeforeEach
    public void before() {
        encoder.setContext(new LoggerContext());
        encoder.setOriginHost("localhost");
    }

    @Test
    public void custom() throws IOException {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);
        event.setTimeStamp(TIMESTAMP);
        event.setThreadName(THREAD_NAME);
        final String logMsg = encodeToStr(event);

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);

        assertEquals("message 1\n", jsonNode.get("full_message").textValue());
        assertEquals(
            "970db79831490f2e5e02b80cf484308bf77e7353f727b6fd42417b82be254419",
            jsonNode.get("_sha256").textValue(),
            "Log line: " + logMsg
        );
    }

    private String encodeToStr(final LoggingEvent event) {
        return new String(encoder.encode(event), StandardCharsets.UTF_8);
    }

}
