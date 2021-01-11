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

import java.util.Map;
import java.util.function.BiConsumer;

import ch.qos.logback.classic.spi.ILoggingEvent;
import de.siegmar.logbackgelf.GelfFieldHelper;
import de.siegmar.logbackgelf.GelfFieldMapper;

public class MdcDataFieldMapper implements GelfFieldMapper<Object> {

    private final GelfFieldHelper fieldHelper;

    public MdcDataFieldMapper(final GelfFieldHelper fieldHelper) {
        this.fieldHelper = fieldHelper;
    }

    @Override
    public void mapField(final ILoggingEvent event, final BiConsumer<String, Object> valueHandler) {
        final Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties == null || mdcProperties.isEmpty()) {
            return;
        }

        for (final Map.Entry<String, String> entry : mdcProperties.entrySet()) {
            if (fieldHelper.isValidFieldName(entry.getKey())) {
                valueHandler.accept(entry.getKey(), fieldHelper.convertToNumberIfNeeded(entry.getValue()));
            }
        }
    }

}
