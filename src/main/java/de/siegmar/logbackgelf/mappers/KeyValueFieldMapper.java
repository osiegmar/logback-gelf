/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2022 Oliver Siegmar
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

import java.util.Optional;
import java.util.function.BiConsumer;

import ch.qos.logback.classic.spi.ILoggingEvent;
import de.siegmar.logbackgelf.GelfFieldMapper;

public class KeyValueFieldMapper implements GelfFieldMapper<Object> {

    @Override
    public void mapField(final ILoggingEvent event, final BiConsumer<String, Object> valueHandler) {
        Optional.ofNullable(event.getKeyValuePairs())
            .ifPresent(pairs -> pairs.forEach(kvp -> valueHandler.accept(kvp.key, kvp.value)));
    }

}
