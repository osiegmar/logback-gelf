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

import java.util.Random;

/**
 * Supplier implementation for GELF message IDs as used for UDP chunks. Unfortunately the GELF
 * protocol limits the message id length to 8 bytes thus an UUID cannot be used (16 bytes).
 */
public class MessageIdSupplier {

    // static random to keep Spotbugs happy
    private static final Random RANDOM = new Random();
    private int machinePart = RANDOM.nextInt();

    public int getMachinePart() {
        return machinePart;
    }

    public void setMachinePart(final int machinePart) {
        this.machinePart = machinePart;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    public Long get() {
        return (long) machinePart << 32 | System.nanoTime() & 0xffffffffL;
    }

}
