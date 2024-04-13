/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2021 Oliver Siegmar
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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;

class XmlConfigurationTest {

    @ParameterizedTest
    @MethodSource
    void xmlConfiguration(final Path file) throws JoranException {
        final LoggerContext context = configure(file);
        assertThat(context)
            .satisfies(c -> assertThat(filterWarningsErrors(c)).isEmpty());
    }

    static Stream<Path> xmlConfiguration() {
        return Stream.of("udp-config.xml", "tcp-config.xml", "tcp_tls-config.xml", "http-config.xml")
            .map(name -> Path.of("src", "test", "resources", name));
    }

    private LoggerContext configure(final Path file) throws JoranException {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getStatusManager().clear();
        context.reset();

        final JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(file.toFile());

        return context;
    }

    private List<Status> filterWarningsErrors(final LoggerContext context) {
        return context.getStatusManager().getCopyOfStatusList().stream()
            .filter(s -> s.getLevel() > Status.INFO)
            .collect(Collectors.toList());
    }

}
