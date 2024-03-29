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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessageIdSupplierTest {

    @Test
    void random() {
        final MessageIdSupplier mis = new MessageIdSupplier();
        assertThat(mis.getAsLong())
            .isNotZero()
            .isNotEqualTo(mis.getAsLong());
    }

    @Test
    void fixed() {
        final MessageIdSupplier mis = new MessageIdSupplier() {
            @Override
            long random() {
                return 0x776D_11CF_72A6_B233L;
            }

            @Override
            long currentTime() {
                return 0x18B_0B3E_CA4CL;
            }
        };

        assertThat(mis.getAsLong())
            .isEqualTo(0x776D_11CF_72A6_AA4CL);
    }

}
