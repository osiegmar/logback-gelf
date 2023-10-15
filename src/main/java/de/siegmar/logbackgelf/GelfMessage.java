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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Collections;
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
    private final long timestamp;
    private final int level;
    private final Map<String, Object> additionalFields;

    GelfMessage(final String host, final String shortMessage, final String fullMessage,
                final long timestamp, final int level, final Map<String, Object> additionalFields) {
        this.host = Objects.requireNonNull(host, "host must not be null");
        this.shortMessage = Objects.requireNonNull(shortMessage, "shortMessage must not be null");
        this.fullMessage = fullMessage;
        this.timestamp = timestamp;
        this.level = level;
        this.additionalFields =
            Objects.requireNonNull(additionalFields, "additionalFields must not be null");
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

    public long getTimestamp() {
        return timestamp;
    }

    public int getLevel() {
        return level;
    }

    public Map<String, Object> getAdditionalFields() {
        return Collections.unmodifiableMap(additionalFields);
    }

    public void toJSON(final OutputStream bos) {
        try (var jsonEncoder = new SimpleJsonEncoder(new OutputStreamWriter(bos, UTF_8))) {
            jsonEncoder
                .appendToJSON("version", VERSION)
                .appendToJSON("host", host)
                .appendToJSON("short_message", shortMessage);

            if (fullMessage != null && !fullMessage.isEmpty()) {
                jsonEncoder.appendToJSON("full_message", fullMessage);
            }

            jsonEncoder
                .appendToJSONUnquoted("timestamp", timestampToGelfNotation(timestamp))
                .appendToJSONUnquoted("level", level);

            additionalFields.forEach((key, value) ->
                jsonEncoder.appendToJSON('_' + key, value));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private static String timestampToGelfNotation(final long timestamp) {
        return new BigDecimal(timestamp).movePointLeft(3).toPlainString();
    }

}
