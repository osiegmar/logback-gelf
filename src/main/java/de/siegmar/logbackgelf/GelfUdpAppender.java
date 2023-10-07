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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.function.LongSupplier;

import de.siegmar.logbackgelf.compressor.Compressor;

public class GelfUdpAppender extends AbstractGelfAppender {

    /**
     * Maximum size of GELF chunks in bytes. Default chunk size is 508 - this prevents
     * IP packet fragmentation. This is also the recommended minimum.
     */
    private Integer maxChunkSize;

    /**
     * Compression method used (NONE, GZIP or ZLIB). Default: GZIP.
     */
    private CompressionMethod compressionMethod = CompressionMethod.GZIP;

    private LongSupplier messageIdSupplier = new MessageIdSupplier();

    private RobustChannel robustChannel;

    private GelfUdpChunker chunker;

    private AddressResolver addressResolver;

    private Compressor compressor;

    public Integer getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(final Integer maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    public CompressionMethod getCompressionMethod() {
        return compressionMethod;
    }

    public void setCompressionMethod(final CompressionMethod compressionMethod) {
        this.compressionMethod = compressionMethod;
    }

    public LongSupplier getMessageIdSupplier() {
        return messageIdSupplier;
    }

    public void setMessageIdSupplier(final LongSupplier messageIdSupplier) {
        this.messageIdSupplier = messageIdSupplier;
    }

    @Override
    protected void startAppender() throws IOException {
        robustChannel = new RobustChannel();
        chunker = new GelfUdpChunker(messageIdSupplier, maxChunkSize);
        addressResolver = new AddressResolver(getGraylogHost());
        compressor = compressionMethod.getCompressor();
    }

    @Override
    protected void appendMessage(final byte[] binMessage) throws IOException {
        final byte[] messageToSend = compressor.compress(binMessage);

        final InetSocketAddress remote = new InetSocketAddress(addressResolver.resolve(),
                getGraylogPort());

        for (final ByteBuffer chunk : chunker.chunks(messageToSend)) {
            while (chunk.hasRemaining()) {
                robustChannel.send(chunk, remote);
            }
        }
    }

    @Override
    protected void close() throws IOException {
        robustChannel.close();
    }

    private static final class RobustChannel {

        private volatile DatagramChannel channel;
        private volatile boolean stopped;

        RobustChannel() throws IOException {
            this.channel = DatagramChannel.open();
        }

        void send(final ByteBuffer src, final SocketAddress target) throws IOException {
            getChannel().send(src, target);
        }

        private DatagramChannel getChannel() throws IOException {
            DatagramChannel tmp = channel;

            if (!tmp.isOpen()) {
                synchronized (this) {
                    tmp = channel;
                    if (!tmp.isOpen() && !stopped) {
                        tmp = DatagramChannel.open();
                        channel = tmp;
                    }
                }
            }

            return tmp;
        }

        synchronized void close() throws IOException {
            channel.close();
            stopped = true;
        }
    }

}
