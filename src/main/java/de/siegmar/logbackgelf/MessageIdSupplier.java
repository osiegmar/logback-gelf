/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2020 Oliver Siegmar
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

public class MessageIdSupplier {

    protected static final int MESSAGE_ID_LENGTH = 8;
    protected static final int LONG_LENGTH = 8;
    private byte[] machinePart;

    public MessageIdSupplier() {
        try {
            machinePart = InetAddress.getLocalHost().getAddress();
        } catch (final UnknownHostException e) {
            machinePart = new byte[LONG_LENGTH];
            new Random().nextBytes(machinePart);
        }
    }

    protected byte[] getMachinePart() {
        return Arrays.copyOf(machinePart, machinePart.length);
    }

    protected void setMachinePart(final byte[] machinePart) {
        this.machinePart = Arrays.copyOf(machinePart, machinePart.length);
    }

    public byte[] get() {
        return Arrays.copyOf(buildMessageId(), MESSAGE_ID_LENGTH);
    }

    protected byte[] buildMessageId() {
        final ByteBuffer bb = ByteBuffer.allocate(machinePart.length + LONG_LENGTH);
        bb.put(machinePart);
        bb.putLong(System.nanoTime());
        bb.flip();
        return md5(bb.array());
    }

    protected static byte[] md5(final byte[] data) {
        try {
            return MessageDigest.getInstance("MD5").digest(data);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
