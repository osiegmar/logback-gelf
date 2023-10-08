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
import java.io.UncheckedIOException;
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

    SimpleJsonEncoder(final Writer writer) {
        this.writer = writer;
        try {
            writer.write('{');
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Append field with quotes and escape characters added, if required.
     *
     * @return this
     */
    SimpleJsonEncoder appendToJSON(final String key, final Object value) {
        if (closed) {
            throw new IllegalStateException("Encoder already closed");
        }
        if (value != null) {
            try {
                appendKey(key);
                if (value instanceof Number) {
                    writer.write(value.toString());
                } else {
                    writer.write(QUOTE);
                    escapeString(value.toString());
                    writer.write(QUOTE);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
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
    SimpleJsonEncoder appendToJSONUnquoted(final String key, final Object value) {
        if (closed) {
            throw new IllegalStateException("Encoder already closed");
        }
        if (value != null) {
            try {
                appendKey(key);
                writer.write(value.toString());
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return this;
    }

    private void appendKey(final String key) throws IOException {
        if (started) {
            writer.write(',');
        } else {
            started = true;
        }
        writer.write(QUOTE);
        escapeString(key);
        writer.write(QUOTE);
        writer.write(':');
    }

    /**
     * Escape characters in string, if required per RFC-7159 (JSON).
     *
     * @param str string to be escaped.
     */
    @SuppressWarnings("checkstyle:cyclomaticcomplexity")
    private void escapeString(final String str) throws IOException {
        for (int i = 0; i < str.length(); i++) {
            final char ch = str.charAt(i);
            switch (ch) {
                case QUOTE:
                case '\\':
                case '/':
                    writer.write('\\');
                    writer.write(ch);
                    break;
                case '\b':
                    writer.write("\\b");
                    break;
                case '\f':
                    writer.write("\\f");
                    break;
                case '\n':
                    writer.write("\\n");
                    break;
                case '\r':
                    // Graylog doesn't like carriage-return: https://github.com/Graylog2/graylog2-server/issues/4470
                    break;
                case '\t':
                    writer.write("\\t");
                    break;
                default:
                    if (ch < ' ') {
                        writer.write(escapeCharacter(ch));
                    } else {
                        writer.write(ch);
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
            writer.write('}');
            closed = true;
        }
        writer.close();
    }

}
