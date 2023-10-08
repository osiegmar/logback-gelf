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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GelfUdpChunkerTest {

    @Test
    void singleChunk() {
        final GelfUdpChunker chunker = new GelfUdpChunker(new MessageIdSupplier(), null);
        final Iterator<? extends ByteBuffer> chunks =
            chunker.chunks("hello".getBytes(StandardCharsets.UTF_8)).iterator();
        assertThat(chunks.next().array()).asString().isEqualTo("hello");
        assertThat(chunks).isExhausted();
    }

    @Test
    void multipleChunks() {
        final GelfUdpChunker chunker = new GelfUdpChunker(new MessageIdSupplier(), 13);
        final Iterator<? extends ByteBuffer> chunks =
            chunker.chunks("hello".getBytes(StandardCharsets.UTF_8)).iterator();
        expectedChunk(chunks.next().array(), 0, 5, 'h');
        expectedChunk(chunks.next().array(), 1, 5, 'e');
        expectedChunk(chunks.next().array(), 2, 5, 'l');
        expectedChunk(chunks.next().array(), 3, 5, 'l');
        expectedChunk(chunks.next().array(), 4, 5, 'o');
        assertThat(chunks).isExhausted();
    }

    private void expectedChunk(final byte[] data, final int chunkNo, final int chunkCount, final char payload) {
        assertThat(data)
            .startsWith(0x1e, 0x0f)
            .endsWith(chunkNo, chunkCount, payload);
    }

    @Test
    void removeNotPermitted() {
        final GelfUdpChunker chunker = new GelfUdpChunker(new MessageIdSupplier(), 13);
        final Iterator<? extends ByteBuffer> chunks =
            chunker.chunks("hello".getBytes(StandardCharsets.UTF_8)).iterator();

        assertThatThrownBy(chunks::remove)
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {12, 65468})
    void maxChunkSizeLimitRange(final int maxChunkSize) {
        assertThatThrownBy(() -> new GelfUdpChunker(new MessageIdSupplier(), maxChunkSize))
            .isInstanceOf(IllegalArgumentException.class);
    }

}
