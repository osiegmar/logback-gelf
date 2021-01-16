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

import java.util.function.BiConsumer;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Field mapper that can be used to add fields to resulting GELF message, using {@link ILoggingEvent} as input.
 */
public interface GelfFieldMapper<T> {

    /**
     * Map a field (one or more) from {@link ILoggingEvent} to a GELF message.
     *
     * @param event the source log event
     * @param valueHandler the consumer of the field ({@link String} name and value)
     */
    void mapField(ILoggingEvent event, BiConsumer<String, T> valueHandler);

}
