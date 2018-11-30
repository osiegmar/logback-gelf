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
 * Simple JSON encoder with very basic functionality that is required by this library.
 */
class SimpleJsonEncoder {

    private static final char QUOTE = '"';
    private static final String COMMA = ",";
    private static final String EMPTY = "";

    /**
     * Internal buffer.
     */
    private final StringBuilder sb = new StringBuilder(256);

    /**
     * Flag to determine if a comma has to be added on next append execution.
     */
    private boolean started;

    /**
     * Flag set when JSON object is closed by curly brace.
     */
    private boolean closed;

    SimpleJsonEncoder() {
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
                sb.append(value.toString());
            } else if (value instanceof String[]) {
                final String[] values = (String[]) value;
                sb.append("[");
                for (int i = 0; i < values.length; i++) {
                    final String endChar = i < values.length - 1 ? COMMA : EMPTY;
                    sb.append(QUOTE).append(escapeString(values[i])).append(QUOTE).append(endChar);
                }
                sb.append("]");
            } else {
                sb.append(QUOTE).append(escapeString(value.toString())).append(QUOTE);
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
        sb.append(QUOTE).append(escapeString(key)).append(QUOTE).append(':');
    }

    /**
     * Escape characters in string, if required per RFC-7159 (JSON).
     *
     * @param str string to be escaped.
     * @return escaped string.
     */
    @SuppressWarnings("checkstyle:cyclomaticcomplexity")
    private static String escapeString(final String str) {
        final StringBuilder sb = new StringBuilder(str.length());

        for (final char ch : str.toCharArray()) {
            switch (ch) {
                case QUOTE:
                case '\\':
                case '/':
                    sb.append('\\');
                    sb.append(ch);
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
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(ch < ' ' ? escapeCharacter(ch) : ch);
            }
        }

        return sb.toString();
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

    /**
     * Returns the JSON representation of all added fields.
     *
     * @return JSON string.
     */
    @Override
    public String toString() {
        if (!closed) {
            sb.append('}');
            closed = true;
        }
        return sb.toString();
    }

}
