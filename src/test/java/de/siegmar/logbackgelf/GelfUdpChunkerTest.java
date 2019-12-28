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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

public class GelfUdpChunkerTest {

    @Test
    public void singleChunk() {
        final GelfUdpChunker chunker = new GelfUdpChunker();
        final Iterator<? extends ByteBuffer> chunks =
            chunker.chunks("hello".getBytes(StandardCharsets.UTF_8)).iterator();
        final String actual = new String(chunks.next().array(), StandardCharsets.UTF_8);
        assertEquals("hello", actual);
        assertFalse(chunks.hasNext());
    }

    @Test
    public void multipleChunks() {
        final GelfUdpChunker chunker = new GelfUdpChunker(13);
        final Iterator<? extends ByteBuffer> chunks =
            chunker.chunks("hello".getBytes(StandardCharsets.UTF_8)).iterator();
        expectedChunk(chunks.next().array(), 0, 5, 'h');
        expectedChunk(chunks.next().array(), 1, 5, 'e');
        expectedChunk(chunks.next().array(), 2, 5, 'l');
        expectedChunk(chunks.next().array(), 3, 5, 'l');
        expectedChunk(chunks.next().array(), 4, 5, 'o');
        assertFalse(chunks.hasNext());
    }

    private void expectedChunk(final byte[] data, final int chunkNo, final int chunkCount,
                               final char payload) {
        assertEquals(0x1e, data[0]);
        assertEquals(0x0f, data[1]);
        // Skip message id (2-9)
        assertEquals(chunkNo, data[10]);
        assertEquals(chunkCount, data[11]);
        assertEquals(payload, data[12]);
    }

}
