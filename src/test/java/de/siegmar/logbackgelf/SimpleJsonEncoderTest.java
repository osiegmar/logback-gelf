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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class SimpleJsonEncoderTest {

    private final StringWriter writer = new StringWriter();
    private final SimpleJsonEncoder enc;

    SimpleJsonEncoderTest() throws IOException {
        enc = new SimpleJsonEncoder(writer);
    }

    @Test
    void unquoted() throws IOException {
        enc.appendToJSONUnquoted("aaa", 123).close();
        assertThat(writer).hasToString("{\"aaa\":123}");
    }

    @Test
    void string() throws IOException {
        enc.appendToJSON("aaa", "bbb").close();
        assertThat(writer.toString()).hasToString("{\"aaa\":\"bbb\"}");
    }

    @Test
    void number() throws IOException {
        enc.appendToJSON("aaa", 123).close();
        assertThat(writer.toString()).hasToString("{\"aaa\":123}");
    }

    @Test
    void quote() throws IOException {
        enc.appendToJSON("aaa", "\"").close();
        assertThat(writer.toString()).hasToString("{\"aaa\":\"\\\"\"}");
    }

    @Test
    void reverseSolidus() throws IOException {
        enc.appendToJSON("aaa", "\\").close();
        assertThat(writer.toString()).hasToString("{\"aaa\":\"\\\\\"}");
    }

    @Test
    void solidus() throws IOException {
        enc.appendToJSON("aaa", "/").close();
        assertThat(writer.toString()).hasToString("{\"aaa\":\"\\/\"}");
    }

    @Test
    void backspace() throws IOException {
        enc.appendToJSON("aaa", "\b").close();
        assertThat(writer.toString()).hasToString("{\"aaa\":\"\\b\"}");
    }

    @Test
    void formFeed() throws IOException {
        enc.appendToJSON("aaa", "\f").close();
        assertThat(writer.toString()).hasToString("{\"aaa\":\"\\f\"}");
    }

    @Test
    void newline() throws IOException {
        enc.appendToJSON("aaa", "\n").close();
        assertThat(writer.toString()).hasToString("{\"aaa\":\"\\n\"}");
    }

    @Test
    void carriageReturn() throws IOException {
        enc.appendToJSON("aaa", "\r\n").close();
        assertThat(writer.toString()).hasToString("{\"aaa\":\"\\n\"}");
    }

    @Test
    void tab() throws IOException {
        enc.appendToJSON("aaa", "\t").close();
        assertThat(writer.toString()).hasToString("{\"aaa\":\"\\t\"}");
    }

    @Test
    @SuppressWarnings("checkstyle:avoidescapedunicodecharacters")
    void unicode() throws IOException {
        enc.appendToJSON("\u0002", "\u0007\u0019").close();
        assertThat(writer.toString()).hasToString("{\"\\u0002\":\"\\u0007\\u0019\"}");
    }

    @Test
    void multipleFields() throws IOException {
        enc.appendToJSONUnquoted("aaa", 123);
        enc.appendToJSON("bbb", "ccc");
        enc.appendToJSON("ddd", 123);
        enc.close();

        assertThat(writer.toString()).hasToString("{\"aaa\":123,\"bbb\":\"ccc\",\"ddd\":123}");
    }

    @Test
    void appendToJSONClosed() throws IOException {
        enc.close();

        assertThatThrownBy(() -> enc.appendToJSON("field", "value"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void appendToUnquotedJSONClosed() throws IOException {
        enc.close();

        assertThatThrownBy(() -> enc.appendToJSONUnquoted("field", "value"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void multipleCloses() throws IOException {
        enc.close();
        assertThatNoException().isThrownBy(enc::close);
    }

    @Test
    void ignoreNullValues() throws IOException {
        enc.appendToJSONUnquoted("key1", null);
        enc.appendToJSON("key2", null);
        enc.appendToJSONUnquoted("key3", 123);
        enc.appendToJSON("key4", "321");
        enc.close();

        assertThat(writer.toString()).hasToString("{\"key3\":123,\"key4\":\"321\"}");
    }

}
