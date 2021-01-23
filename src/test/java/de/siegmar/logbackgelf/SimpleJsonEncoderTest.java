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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

public class SimpleJsonEncoderTest {

    private final StringWriter writer = new StringWriter();
    private final SimpleJsonEncoder enc;

    public SimpleJsonEncoderTest() throws IOException {
        enc = new SimpleJsonEncoder(writer);
    }

    @Test
    public void unquoted() throws IOException {
        enc.appendToJSONUnquoted("aaa", 123).close();
        assertEquals("{\"aaa\":123}", writer.toString());
    }

    @Test
    public void string() throws IOException {
        enc.appendToJSON("aaa", "bbb").close();
        assertEquals("{\"aaa\":\"bbb\"}", writer.toString());
    }

    @Test
    public void number() throws IOException {
        enc.appendToJSON("aaa", 123).close();
        assertEquals("{\"aaa\":123}", writer.toString());
    }

    @Test
    public void quote() throws IOException {
        enc.appendToJSON("aaa", "\"").close();
        assertEquals("{\"aaa\":\"\\\"\"}", writer.toString());
    }

    @Test
    public void reverseSolidus() throws IOException {
        enc.appendToJSON("aaa", "\\").close();
        assertEquals("{\"aaa\":\"\\\\\"}", writer.toString());
    }

    @Test
    public void solidus() throws IOException {
        enc.appendToJSON("aaa", "/").close();
        assertEquals("{\"aaa\":\"\\/\"}", writer.toString());
    }

    @Test
    public void backspace() throws IOException {
        enc.appendToJSON("aaa", "\b").close();
        assertEquals("{\"aaa\":\"\\b\"}", writer.toString());
    }

    @Test
    public void formFeed() throws IOException {
        enc.appendToJSON("aaa", "\f").close();
        assertEquals("{\"aaa\":\"\\f\"}", writer.toString());
    }

    @Test
    public void newline() throws IOException {
        enc.appendToJSON("aaa", "\n").close();
        assertEquals("{\"aaa\":\"\\n\"}", writer.toString());
    }

    @Test
    public void carriageReturn() throws IOException {
        enc.appendToJSON("aaa", "\r").close();
        assertEquals("{\"aaa\":\"\\r\"}", writer.toString());
    }

    @Test
    public void tab() throws IOException {
        enc.appendToJSON("aaa", "\t").close();
        assertEquals("{\"aaa\":\"\\t\"}", writer.toString());
    }

    @Test
    @SuppressWarnings("checkstyle:avoidescapedunicodecharacters")
    public void unicode() throws IOException {
        enc.appendToJSON("\u0002", "\u0007\u0019").close();
        assertEquals("{\"\\u0002\":\"\\u0007\\u0019\"}", writer.toString());
    }

    @Test
    public void multipleFields() throws IOException {
        enc.appendToJSONUnquoted("aaa", 123);
        enc.appendToJSON("bbb", "ccc");
        enc.appendToJSON("ddd", 123);
        enc.close();

        assertEquals("{\"aaa\":123,\"bbb\":\"ccc\",\"ddd\":123}", writer.toString());
    }

}
