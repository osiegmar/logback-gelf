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

/**
 * This class is responsible for read structured arguments.
 */
public final class StructuredArgs {

    /**
     * Argument key.
     */
    private final String key;

    /**
     * Argument value.
     */
    private final Object object;

    private StructuredArgs(final String key, final Object object) {
        this.key = key;
        this.object = object;
    }

    public String getKey() {
        return key;
    }

    public Object getObject() {
        return object;
    }

    public static StructuredArgs value(final String key, final Object value) {
        return new StructuredArgs(key, value);
    }

    public static StructuredArgs kv(final String key, final Object value) {
        return value(key, value);
    }

    @Override
    public String toString() {
        return object.toString();
    }
}
