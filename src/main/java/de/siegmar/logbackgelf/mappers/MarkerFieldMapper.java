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

import java.util.Iterator;

import org.slf4j.Marker;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class MarkerFieldMapper extends AbstractFixedNameFieldMapper<String> {

    public MarkerFieldMapper(final String fieldName) {
        super(fieldName);
    }

    @Override
    protected String getValue(final ILoggingEvent event) {
        final Marker marker = event.getMarker();
        return marker != null ? buildMarkerStr(marker) : null;
    }

    private static String buildMarkerStr(final Marker marker) {
        if (!marker.hasReferences()) {
            return marker.getName();
        }

        final StringBuilder sb = new StringBuilder(marker.getName());

        final Iterator<Marker> it = marker.iterator();
        do {
            sb.append(", ").append(it.next().getName());
        } while (it.hasNext());

        return sb.toString();
    }

}
