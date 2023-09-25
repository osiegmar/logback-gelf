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

package de.siegmar.logbackgelf.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.spi.LoggingEvent;

class MarkerFieldMapperTest {
    private final MarkerFieldMapper markerFieldMapper = new MarkerFieldMapper("marker");

    @Test
    void multipleMarkers() {
        final LoggingEvent loggingEvent = new LoggingEvent();
        final Marker first = MarkerFactory.getMarker("first");
        first.add(MarkerFactory.getMarker("reference"));
        loggingEvent.addMarker(first);
        loggingEvent.addMarker(MarkerFactory.getMarker("second"));

        final Map<String, String> result = new HashMap<>();
        markerFieldMapper.mapField(loggingEvent, result::put);

        assertEquals(1, result.size());
        assertEquals("first, reference, second", result.get("marker"));
    }

    @Test
    void noMarkers() {
        final LoggingEvent loggingEvent = new LoggingEvent();

        final Map<String, String> result = new HashMap<>();
        markerFieldMapper.mapField(loggingEvent, result::put);

        assertEquals(0, result.size());
    }
}
