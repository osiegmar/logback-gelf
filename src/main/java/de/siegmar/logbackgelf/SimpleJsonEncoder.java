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

/**
 * Simple JSON encoder with very basic functionality that is required by this library.
 */
class SimpleJsonEncoder implements Closeable {

    private static final char QUOTE = '"';

    /**
     * Wrapped writer.
     */
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder sb;

    /**
     * Flag to determine if a comma has to be added on next append execution.
     */
    private boolean started;

    /**
     * Flag set when JSON object is closed by curly brace.
     */
    private boolean closed;

    SimpleJsonEncoder(final StringBuilder sb) {
        this.sb = sb;
        sb.append('{');
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
            appendKey(key);
            if (value instanceof Number) {
                sb.append(value);
            } else {
                sb.append(QUOTE);
                escapeString(value.toString());
                sb.append(QUOTE);
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
            appendKey(key);
            sb.append(value);
        }
        return this;
    }

    private void appendKey(final String key) {
        if (started) {
            sb.append(',');
        } else {
            started = true;
        }
        sb.append(QUOTE);
        escapeString(key);
        sb.append(QUOTE).append(':');
    }

    /**
     * Escape characters in string, if required per RFC-7159 (JSON).
     *
     * @param str string to be escaped.
     */
    @SuppressWarnings({
        "checkstyle:cyclomaticcomplexity",
        "PMD.ImplicitSwitchFallThrough",
        "PMD.AvoidLiteralsInIfCondition"
    })
    private void escapeString(final String str) {
        for (int i = 0; i < str.length(); i++) {
            final char ch = str.charAt(i);
            switch (ch) {
                case QUOTE:
                case '\\':
                case '/':
                    sb.append('\\').append(ch);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    // Graylog doesn't like carriage-return: https://github.com/Graylog2/graylog2-server/issues/4470
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (ch < ' ') {
                        sb.append(escapeCharacter(ch));
                    } else {
                        sb.append(ch);
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
    @SuppressWarnings({"checkstyle:magicnumber", "PMD.AvoidLiteralsInIfCondition"})
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
    public void close() {
        if (!closed) {
            sb.append('}');
            closed = true;
        }
    }

}
