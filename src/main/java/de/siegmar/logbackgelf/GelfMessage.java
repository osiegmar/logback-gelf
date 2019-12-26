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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Class for GELF 1.1 format representation.
 */
public class GelfMessage {

    private static final String VERSION = "1.1";

    private final String host;
    private final String shortMessage;
    private final String fullMessage;
    private final double timestamp;
    private final int level;
    private final Map<String, Object> additionalFields;

    GelfMessage(final String host, final String shortMessage, final String fullMessage,
                final double timestamp, final int level,
                final Map<String, Object> additionalFields) {
        this.host = Objects.requireNonNull(host, "host must not be null");
        this.shortMessage = Objects.requireNonNull(shortMessage, "shortMessage must not be null");
        this.fullMessage = fullMessage == null || fullMessage.isEmpty() ? null : fullMessage;
        this.timestamp = timestamp;
        this.level = level;
        this.additionalFields =
            Objects.requireNonNull(additionalFields, "additionalFields must not be null");
    }

    String toJSON() {
        final SimpleJsonEncoder jsonEncoder = new SimpleJsonEncoder();

        final DecimalFormat decimalFormat =
            new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        jsonEncoder
            .appendToJSON("version", VERSION)
            .appendToJSON("host", host)
            .appendToJSON("short_message", shortMessage)
            .appendToJSON("full_message", fullMessage)
            .appendToJSONUnquoted("timestamp", decimalFormat.format(timestamp))
            .appendToJSONUnquoted("level", level);

        for (final Map.Entry<String, Object> entry : additionalFields.entrySet()) {
            jsonEncoder.appendToJSON('_' + entry.getKey(), entry.getValue());
        }

        return jsonEncoder.toString();
    }

    public String getHost() {
        return host;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public String getFullMessage() {
        return fullMessage;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public int getLevel() {
        return level;
    }

    public Map<String, Object> getAdditionalFields() {
        return additionalFields;
    }

}
