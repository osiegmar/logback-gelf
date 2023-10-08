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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HostnameVerifierTest {

    @Test
    void testSimple() {
        assertThat(HostnameVerifier.verify("foo.com", "foo.com")).isTrue();
        assertThat(HostnameVerifier.verify("foo", "foo.com")).isFalse();
    }

    @Test
    void testWildcard() {
        assertThat(HostnameVerifier.verify("foo.a.com", "*.a.com")).isTrue();
        assertThat(HostnameVerifier.verify("bar.foo.a.com", "*.a.com")).isFalse();
    }

    @Test
    void testPartial() {
        // Partial-wildcard is not supported
        assertThat(HostnameVerifier.verify("foo.com", "f*.com")).isFalse();
    }

    @Test
    void invalidCN() {
        assertThat(HostnameVerifier.verify("foo.com", "*")).isFalse();
        assertThat(HostnameVerifier.verify("foo.com", "*.com")).isFalse();
        assertThat(HostnameVerifier.verify("foo.a.com", "*.*.com")).isFalse();
        assertThat(HostnameVerifier.verify("foo.a.com", "foo.*.com")).isFalse();
        assertThat(HostnameVerifier.verify("foo.com", "foo.*")).isFalse();
    }

    @Test
    void testCase() {
        assertThat(HostnameVerifier.verify("foo.COM", "FOO.com")).isTrue();
        assertThat(HostnameVerifier.verify("FOO.a.com", "*.a.COM")).isTrue();
    }

    @Test
    void testPuny() {
        assertThat(HostnameVerifier.verify("www.xn--caf-dma.com", "*.xn--caf-dma.com")).isTrue();
        assertThat(HostnameVerifier.verify("WWW.xn--CAF-dma.com", "*.xn--caf-DMA.COM")).isTrue();
    }

    @Test
    void testNull() {
        assertThatThrownBy(() -> HostnameVerifier.verify(null, "foo.com"))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HostnameVerifier.verify("foo.com", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testEmpty() {
        assertThat(HostnameVerifier.verify("", "foo.com")).isFalse();
        assertThat(HostnameVerifier.verify("foo.com", "")).isFalse();
    }

}
