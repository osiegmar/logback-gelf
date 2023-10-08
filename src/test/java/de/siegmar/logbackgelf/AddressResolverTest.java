/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2018 Oliver Siegmar
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class AddressResolverTest {

    @Test
    void test() throws UnknownHostException {
        final AddressResolver resolver =
            new AddressResolver("foo", new AtomicInteger(Integer.MAX_VALUE)) {
                @Override
                protected InetAddress[] lookup() throws UnknownHostException {
                    return new InetAddress[]{
                        InetAddress.getByName("127.0.0.1"),
                        InetAddress.getByName("8.8.8.8"),
                    };
                }
            };

        assertThat(resolver.resolve().getHostAddress()).isEqualTo("8.8.8.8");
        assertThat(resolver.resolve().getHostAddress()).isEqualTo("127.0.0.1");
        assertThat(resolver.resolve().getHostAddress()).isEqualTo("8.8.8.8");
    }

}
