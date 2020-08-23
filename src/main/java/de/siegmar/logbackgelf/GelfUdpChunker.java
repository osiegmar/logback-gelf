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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

class GelfUdpChunker {

    /**
     * Maximum number of chunks, as defined per GELF Format Specification.
     */
    private static final int MAX_CHUNKS = 128;

    /**
     * GELF chunk header, as defined per GELF Format Specification.
     */
    private static final byte[] CHUNKED_GELF_HEADER = new byte[] {0x1e, 0x0f};

    /**
     * Length of message ID field, as defined per GELF Format Specification.
     */
    private static final int MESSAGE_ID_LENGTH = 8;

    /**
     * Length of sequence number field, as defined per GELF Format Specification.
     */
    private static final int SEQ_COUNT_LENGTH = 2;

    /**
     * Sum of all header fields.
     */
    private static final int HEADER_LENGTH =
        CHUNKED_GELF_HEADER.length + MESSAGE_ID_LENGTH + SEQ_COUNT_LENGTH;

    private static final int MIN_CHUNK_SIZE = HEADER_LENGTH + 1;

    /**
     * Default chunk size set to 508 bytes. This prevents IP packet fragmentation.
     *
     * Minimum MTU (576) - IP header (up to 60) - UDP header (8) = 508
     */
    private static final int DEFAULT_CHUNK_SIZE = 508;

    /**
     * Maximum chunk size set to 65467 bytes.
     *
     * Maximum IP packet size (65535) - IP header (up to 60) - UDP header (8) = 65467
     */
    private static final int MAX_CHUNK_SIZE = 65467;

    private static final int MAX_CHUNK_PAYLOAD_SIZE = MAX_CHUNK_SIZE - HEADER_LENGTH;

    /**
     * The maximum size used for the payload.
     */
    private final int maxChunkPayloadSize;

    private final MessageIdSupplier messageIdSupplier;

    GelfUdpChunker(final MessageIdSupplier messageIdSupplier, final Integer maxChunkSize) {
        this.messageIdSupplier = messageIdSupplier;

        if (maxChunkSize != null) {
            if (maxChunkSize < MIN_CHUNK_SIZE) {
                throw new IllegalArgumentException("Minimum chunk size is " + MIN_CHUNK_SIZE);
            }

            if (maxChunkSize > MAX_CHUNK_SIZE) {
                throw new IllegalArgumentException("Maximum chunk size is " + MAX_CHUNK_SIZE);
            }
        }

        final int mcs = maxChunkSize != null ? maxChunkSize : DEFAULT_CHUNK_SIZE;
        this.maxChunkPayloadSize = mcs - HEADER_LENGTH;
    }

    private static ByteBuffer buildChunk(final long messageId, final byte[] message,
                                         final byte chunkCount, final byte chunkNo,
                                         final int maxChunkPayloadSize) {

        final int chunkPayloadSize =
            Math.min(maxChunkPayloadSize, message.length - chunkNo * maxChunkPayloadSize);

        final ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_LENGTH + chunkPayloadSize);

        // Chunked GELF magic bytes 2 bytes
        byteBuffer.put(CHUNKED_GELF_HEADER);

        // Message ID 8 bytes
        byteBuffer.putLong(messageId);

        // Sequence number 1 byte
        byteBuffer.put(chunkNo);

        // Sequence count 1 byte
        byteBuffer.put(chunkCount);

        // message
        byteBuffer.put(message, chunkNo * maxChunkPayloadSize, chunkPayloadSize);

        byteBuffer.flip();

        return byteBuffer;
    }

    Iterable<? extends ByteBuffer> chunks(final byte[] message) {
        return (Iterable<ByteBuffer>) () -> new ChunkIterator(message);
    }

    private final class ChunkIterator implements Iterator<ByteBuffer> {

        private final byte[] message;
        private final int chunkSize;
        private final byte chunkCount;
        private final long messageId;

        private byte chunkIdx;

        private ChunkIterator(final byte[] message) {
            this.message = message;

            int localChunkSize = maxChunkPayloadSize;
            int localChunkCount = calcChunkCount(message, localChunkSize);

            if (localChunkCount > MAX_CHUNKS) {
                // Number of chunks would exceed maximum chunk limit - use a larger chunk size
                // as a last resort.

                localChunkSize = MAX_CHUNK_PAYLOAD_SIZE;
                localChunkCount = calcChunkCount(message, localChunkSize);
            }

            if (localChunkCount > MAX_CHUNKS) {
                throw new IllegalArgumentException("Message to big (" + message.length + " B)");
            }

            this.chunkSize = localChunkSize;
            this.chunkCount = (byte) localChunkCount;

            messageId = localChunkCount > 1 ? messageIdSupplier.get() : 0;
        }

        private int calcChunkCount(final byte[] msg, final int cs) {
            return (msg.length + cs - 1) / cs;
        }

        @Override
        public boolean hasNext() {
            return chunkIdx < chunkCount;
        }

        @Override
        public ByteBuffer next() {
            if (!hasNext()) {
                throw new NoSuchElementException("All " + chunkCount + " chunks consumed");
            }

            if (chunkCount == 1) {
                chunkIdx++;
                return ByteBuffer.wrap(message);
            }

            return buildChunk(messageId, message, chunkCount, chunkIdx++, chunkSize);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

    }

}
