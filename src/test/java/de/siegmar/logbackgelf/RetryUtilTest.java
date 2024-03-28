/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2024 Oliver Siegmar
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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class RetryUtilTest {

    private final AtomicInteger counter = new AtomicInteger();

    @Test
    void shouldRetryUntilMaxRetries() {
        assertThatThrownBy(() -> RetryUtil.retry(() -> incCounter(counter), () -> true, 5, 1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Retry");
        assertThat(counter).hasValue(6);
    }

    @Test
    void shouldRetryUntilMaxRetriesWhenConditionIsFalse() {
        assertThatThrownBy(() -> RetryUtil.retry(() -> incCounter(counter), () -> false, 5, 1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Retry");
        assertThat(counter).hasValue(1);
    }

    private static int incCounter(final AtomicInteger counter) {
        final int i = counter.incrementAndGet();
        if (i > 0) {
            throw new IllegalStateException("Retry");
        }
        return i;
    }

}
