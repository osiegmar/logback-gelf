/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2019 Oliver Siegmar
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HostnameVerifierTest {

    @Test
    public void testSimple() {
        assertTrue(HostnameVerifier.verify("foo.com", "foo.com"));
        assertFalse(HostnameVerifier.verify("foo", "foo.com"));
    }

    @Test
    public void testWildcard() {
        assertTrue(HostnameVerifier.verify("foo.a.com", "*.a.com"));
        assertFalse(HostnameVerifier.verify("bar.foo.a.com", "*.a.com"));
    }

    @Test
    public void testPartial() {
        // Partial-wildcard is not supported
        assertFalse(HostnameVerifier.verify("foo.com", "f*.com"));
    }

    @Test
    public void invalidCN() {
        assertFalse(HostnameVerifier.verify("foo.com", "*"));
        assertFalse(HostnameVerifier.verify("foo.com", "*.com"));
        assertFalse(HostnameVerifier.verify("foo.a.com", "*.*.com"));
        assertFalse(HostnameVerifier.verify("foo.a.com", "foo.*.com"));
        assertFalse(HostnameVerifier.verify("foo.com", "foo.*"));
    }

    @Test
    public void testCase() {
        assertTrue(HostnameVerifier.verify("foo.COM", "FOO.com"));
        assertTrue(HostnameVerifier.verify("FOO.a.com", "*.a.COM"));
    }

    @Test
    public void testPuny() {
        assertTrue(HostnameVerifier.verify("www.xn--caf-dma.com", "*.xn--caf-dma.com"));
        assertTrue(HostnameVerifier.verify("WWW.xn--CAF-dma.com", "*.xn--caf-DMA.COM"));
    }

    @Test
    public void testNull() {
        assertThrows(NullPointerException.class, () -> HostnameVerifier.verify(null, "foo.com"));
        assertThrows(NullPointerException.class, () -> HostnameVerifier.verify("foo.com", null));
    }

    @Test
    public void testEmpty() {
        HostnameVerifier.verify("", "foo.com");
        HostnameVerifier.verify("foo.com", "");
    }

}
