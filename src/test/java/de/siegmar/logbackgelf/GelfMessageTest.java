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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GelfMessageTest {

    @Test
    void simple() {
        final Map<String, Object> additionalFields = Map.of("foo", "bar");

        final GelfMessage message = new GelfMessage("host", "short message", null,
            1584271169123L, 6, additionalFields);

        assertThat(message).satisfies(
            a -> assertThat(a.getHost()).isEqualTo("host"),
            a -> assertThat(a.getShortMessage()).isEqualTo("short message"),
            a -> assertThat(a.getFullMessage()).isNull(),
            a -> assertThat(a.getTimestamp()).isEqualTo(1584271169123L),
            a -> assertThat(a.getLevel()).isEqualTo(6),
            a -> assertThat(a.getAdditionalFields()).isEqualTo(additionalFields),
            a -> assertThat(toJSON(a)).asString().isEqualTo(
                "{"
                + "\"version\":\"1.1\","
                + "\"host\":\"host\","
                + "\"short_message\":\"short message\","
                + "\"timestamp\":1584271169.123,"
                + "\"level\":6,"
                + "\"_foo\":\"bar\""
                + "}")
        );
    }

    @Test
    void complete() {
        final Map<String, Object> additionalFields = Map.of("foo", "bar");

        final GelfMessage message = new GelfMessage("host", "short message", "full message",
            1584271169123L, 6, additionalFields);

        assertThat(message).satisfies(
            a -> assertThat(a.getHost()).isEqualTo("host"),
            a -> assertThat(a.getShortMessage()).isEqualTo("short message"),
            a -> assertThat(a.getFullMessage()).isEqualTo("full message"),
            a -> assertThat(a.getTimestamp()).isEqualTo(1584271169123L),
            a -> assertThat(a.getLevel()).isEqualTo(6),
            a -> assertThat(a.getAdditionalFields()).isEqualTo(additionalFields),
            a -> assertThat(toJSON(a)).asString().isEqualTo(
                "{"
                + "\"version\":\"1.1\","
                + "\"host\":\"host\","
                + "\"short_message\":\"short message\","
                + "\"full_message\":\"full message\","
                + "\"timestamp\":1584271169.123,"
                + "\"level\":6,"
                + "\"_foo\":\"bar\""
                + "}")
        );
    }

    @Test
    void filterEmptyFullMessage() {
        final GelfMessage message = new GelfMessage("host", "short message", "",
            1584271169123L, 6, Map.of());

        assertThat(message).satisfies(
            a -> assertThat(a.getShortMessage()).isEqualTo("short message"),
            a -> assertThat(a.getFullMessage()).isNull()
        );
    }

    private byte[] toJSON(final GelfMessage gelfMessage) {
        final var bos = new ByteArrayOutputStream();
        gelfMessage.toJSON(bos);
        return bos.toByteArray();
    }

}
