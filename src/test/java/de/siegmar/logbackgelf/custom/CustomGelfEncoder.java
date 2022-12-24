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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.util.encoders.Hex;

import de.siegmar.logbackgelf.GelfEncoder;
import de.siegmar.logbackgelf.GelfMessage;

// Put it in different package from GelfEncoder to reveal any visibility issues
public class CustomGelfEncoder extends GelfEncoder {

    @Override
    protected byte[] gelfMessageToJson(final GelfMessage gelfMessage) {
        final byte[] json = super.gelfMessageToJson(gelfMessage);
        gelfMessage.getAdditionalFields().put("sha256", sha256(json));
        return super.gelfMessageToJson(gelfMessage);
    }

    private static String sha256(final byte[] data) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(data);
            return new String(Hex.encode(hash), StandardCharsets.US_ASCII);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
