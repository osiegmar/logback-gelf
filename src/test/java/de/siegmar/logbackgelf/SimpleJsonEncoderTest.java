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

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class SimpleJsonEncoderTest {

    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder sb = new StringBuilder();
    private final SimpleJsonEncoder enc;

    SimpleJsonEncoderTest() {
        enc = new SimpleJsonEncoder(sb);
    }

    @Test
    void unquoted() {
        enc.appendToJSONUnquoted("aaa", 123).close();
        assertThat(sb).hasToString("{\"aaa\":123}");
    }

    @Test
    void string() {
        enc.appendToJSON("aaa", "bbb").close();
        assertThat(sb).hasToString("{\"aaa\":\"bbb\"}");
    }

    @Test
    void number() {
        enc.appendToJSON("aaa", 123).close();
        assertThat(sb).hasToString("{\"aaa\":123}");
    }

    @Test
    void quote() {
        enc.appendToJSON("aaa", "\"").close();
        assertThat(sb).hasToString("{\"aaa\":\"\\\"\"}");
    }

    @Test
    void reverseSolidus() {
        enc.appendToJSON("aaa", "\\").close();
        assertThat(sb).hasToString("{\"aaa\":\"\\\\\"}");
    }

    @Test
    void solidus() {
        enc.appendToJSON("aaa", "/").close();
        assertThat(sb).hasToString("{\"aaa\":\"\\/\"}");
    }

    @Test
    void backspace() {
        enc.appendToJSON("aaa", "\b").close();
        assertThat(sb).hasToString("{\"aaa\":\"\\b\"}");
    }

    @Test
    void formFeed() {
        enc.appendToJSON("aaa", "\f").close();
        assertThat(sb).hasToString("{\"aaa\":\"\\f\"}");
    }

    @Test
    void newline() {
        enc.appendToJSON("aaa", "\n").close();
        assertThat(sb).hasToString("{\"aaa\":\"\\n\"}");
    }

    @Test
    void carriageReturn() {
        enc.appendToJSON("aaa", "\r\n").close();
        assertThat(sb).hasToString("{\"aaa\":\"\\n\"}");
    }

    @Test
    void tab() throws IOException {
        enc.appendToJSON("aaa", "\t").close();
        assertThat(sb).hasToString("{\"aaa\":\"\\t\"}");
    }

    @Test
    @SuppressWarnings("checkstyle:avoidescapedunicodecharacters")
    void unicode() {
        enc.appendToJSON("\u0002", "\u0007\u0019").close();
        assertThat(sb).hasToString("{\"\\u0002\":\"\\u0007\\u0019\"}");
    }

    @Test
    void multipleFields() {
        enc.appendToJSONUnquoted("aaa", 123);
        enc.appendToJSON("bbb", "ccc");
        enc.appendToJSON("ddd", 123);
        enc.close();

        assertThat(sb).hasToString("{\"aaa\":123,\"bbb\":\"ccc\",\"ddd\":123}");
    }

    @Test
    void appendToJSONClosed() {
        enc.close();

        assertThatThrownBy(() -> enc.appendToJSON("field", "value"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void appendToUnquotedJSONClosed() {
        enc.close();

        assertThatThrownBy(() -> enc.appendToJSONUnquoted("field", "value"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void multipleCloses() {
        enc.close();
        assertThatNoException().isThrownBy(enc::close);
    }

    @Test
    void ignoreNullValues() {
        enc.appendToJSONUnquoted("key1", null);
        enc.appendToJSON("key2", null);
        enc.appendToJSONUnquoted("key3", 123);
        enc.appendToJSON("key4", "321");
        enc.close();

        assertThat(sb).hasToString("{\"key3\":123,\"key4\":\"321\"}");
    }

}
