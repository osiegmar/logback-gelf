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

package de.siegmar.logbackgelf.custom;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Map;

import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.util.encoders.Hex;

import de.siegmar.logbackgelf.GelfEncoder;
import de.siegmar.logbackgelf.GelfMessage;

// Put it in different package from GelfEncoder to reveal any visibility issues
public class CustomGelfEncoder extends GelfEncoder {

    @Override
    protected GelfMessage buildGelfMessage(final long timestamp, final int logLevel, final String shortMessage,
                                           final String fullMessage, final Map<String, Object> additionalFields) {
        final GelfMessage gelfMessage =
            super.buildGelfMessage(timestamp, logLevel, shortMessage, fullMessage, additionalFields);

        additionalFields.put("sha256", buildHash(gelfMessage));

        return super.buildGelfMessage(timestamp, logLevel, shortMessage, fullMessage, additionalFields);
    }

    private static String buildHash(final GelfMessage gelfMessage) {
        final MessageDigest digest = new SHA256.Digest();

        try (DigestOutputStream dos = new DigestOutputStream(OutputStream.nullOutputStream(), digest)) {
            gelfMessage.appendJSON(dos);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return toHex(digest.digest());
    }

    private static String toHex(final byte[] data) {
        return new String(Hex.encode(data), StandardCharsets.US_ASCII);
    }

}
