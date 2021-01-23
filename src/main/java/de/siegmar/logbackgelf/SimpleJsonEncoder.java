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

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

/**
 * Simple JSON encoder with very basic functionality that is required by this library.
 */
class SimpleJsonEncoder implements Closeable {

    private static final char QUOTE = '"';

    /**
     * Wrapped writer.
     */
    private final Writer writer;

    /**
     * Flag to determine if a comma has to be added on next append execution.
     */
    private boolean started;

    /**
     * Flag set when JSON object is closed by curly brace.
     */
    private boolean closed;

    SimpleJsonEncoder(final Writer writer) throws IOException {
        this.writer = writer;
        writer.append('{');
    }

    /**
     * Append field with quotes and escape characters added, if required.
     *
     * @return this
     */
    SimpleJsonEncoder appendToJSON(final String key, final Object value) throws IOException {
        if (closed) {
            throw new IllegalStateException("Encoder already closed");
        }
        if (value != null) {
            appendKey(key);
            if (value instanceof Number) {
                writer.append(value.toString());
            } else {
                writer.append(QUOTE);
                escapeString(value.toString());
                writer.append(QUOTE);
            }
        }
        return this;
    }

    /**
     * Append field with quotes and escape characters added in the key, if required.
     * The value is added without quotes and any escape characters.
     *
     * @return this
     */
    SimpleJsonEncoder appendToJSONUnquoted(final String key, final Object value) throws IOException {
        if (closed) {
            throw new IllegalStateException("Encoder already closed");
        }
        if (value != null) {
            appendKey(key);
            writer.append(value.toString());
        }
        return this;
    }

    private void appendKey(final String key) throws IOException {
        if (started) {
            writer.append(',');
        } else {
            started = true;
        }
        writer.append(QUOTE);
        escapeString(key);
        writer.append(QUOTE).append(':');
    }

    /**
     * Escape characters in string, if required per RFC-7159 (JSON).
     *
     * @param str string to be escaped.
     */
    @SuppressWarnings("checkstyle:cyclomaticcomplexity")
    private void escapeString(final String str) throws IOException {
        for (final char ch : str.toCharArray()) {
            switch (ch) {
                case QUOTE:
                case '\\':
                case '/':
                    writer.append('\\');
                    writer.append(ch);
                    break;
                case '\b':
                    writer.append("\\b");
                    break;
                case '\f':
                    writer.append("\\f");
                    break;
                case '\n':
                    writer.append("\\n");
                    break;
                case '\r':
                    writer.append("\\r");
                    break;
                case '\t':
                    writer.append("\\t");
                    break;
                default:
                    if (ch < ' ') {
                        writer.append(escapeCharacter(ch));
                    } else {
                        writer.append(ch);
                    }
            }
        }
    }

    /**
     * Escapes character to unicode string representation (&#92;uXXXX).
     *
     * @param ch character to be escaped.
     * @return escaped representation of character.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    private static String escapeCharacter(final char ch) {
        final String prefix;

        if (ch < 0x10) {
            prefix = "000";
        } else if (ch < 0x100) {
            prefix = "00";
        } else if (ch < 0x1000) {
            prefix = "0";
        } else {
            prefix = "";
        }

        return "\\u" + prefix + Integer.toHexString(ch);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            writer.append('}');
            closed = true;
        }
        writer.close();
    }

}
