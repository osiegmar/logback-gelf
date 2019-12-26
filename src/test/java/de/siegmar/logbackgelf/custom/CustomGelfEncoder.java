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

import de.siegmar.logbackgelf.GelfEncoder;
import de.siegmar.logbackgelf.GelfMessage;

// Put it in different package from GelfEncoder to reveal any visibility issues
public class CustomGelfEncoder extends GelfEncoder {

    @Override
    protected String gelfMessageToJson(final GelfMessage gelfMessage) {
        final String json = super.gelfMessageToJson(gelfMessage);
        final String sha256 = sha256(json);
        gelfMessage.getAdditionalFields().put("sha256", sha256);
        return super.gelfMessageToJson(gelfMessage);
    }

    // Based on https://stackoverflow.com/a/11009612/74694
    private static String sha256(final String base) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
