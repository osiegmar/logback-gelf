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

import com.google.common.hash.Hashing;

import de.siegmar.logbackgelf.GelfEncoder;
import de.siegmar.logbackgelf.GelfMessage;

// Put it in different package from GelfEncoder to reveal any visibility issues
public class CustomGelfEncoder extends GelfEncoder {

    @Override
    protected String gelfMessageToJson(final GelfMessage gelfMessage) {
        final String json = super.gelfMessageToJson(gelfMessage);
        final String sha256 = Hashing.sha256().hashString(json, StandardCharsets.UTF_8).toString();
        gelfMessage.getAdditionalFields().put("sha256", sha256);
        return super.gelfMessageToJson(gelfMessage);
    }

}
