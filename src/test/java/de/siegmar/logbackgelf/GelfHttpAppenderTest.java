/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2024 Oliver Siegmar
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

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.awaitility.Awaitility.await;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPattern;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

class GelfHttpAppenderTest {

    private static final String LOGGER_NAME = GelfHttpAppenderTest.class.getCanonicalName();

    @RegisterExtension
    private static final WireMockExtension WIRE_MOCK = WireMockExtension.newInstance()
        .options(WireMockConfiguration.wireMockConfig().dynamicPort())
        .build();

    @Test
    void simple() {
        final RequestPattern request = gelfRequest();

        final Logger logger = setupLogger();

        logger.error("Test message");

        stopLogger(logger);

        final String json = awaitMessage(request);
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

    private static RequestPattern gelfRequest() {
        return WIRE_MOCK.stubFor(post("/gelf").willReturn(ok()))
            .getRequest();
    }

    private static String awaitMessage(final RequestPattern request) {
        await().until(() -> WIRE_MOCK.countRequestsMatching(request).getCount() > 0);
        return WIRE_MOCK.findRequestsMatching(request).getRequests().get(0).getBodyAsString();
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

    private GelfHttpAppender buildAppender(final LoggerContext lc, final GelfEncoder gelfEncoder) {
        final GelfHttpAppender gelfAppender = new GelfHttpAppender();
        gelfAppender.setContext(lc);
        gelfAppender.setName("GELF");
        gelfAppender.setUri(URI.create(String.format("http://localhost:%d/gelf", WIRE_MOCK.getPort())));
        gelfAppender.setEncoder(gelfEncoder);
        gelfAppender.start();
        return gelfAppender;
    }

    private void stopLogger(final Logger logger) {
        final GelfHttpAppender gelfAppender = (GelfHttpAppender) logger.getAppender("GELF");
        gelfAppender.stop();
    }

}
