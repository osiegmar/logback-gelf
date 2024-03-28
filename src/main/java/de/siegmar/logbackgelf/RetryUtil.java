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

import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;

final class RetryUtil {

    private RetryUtil() {
        // Utility class
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static <T> T retry(final Callable<T> action, final BooleanSupplier retryCondition, final int maxRetries,
                              final long retryDelay) {
        int retryCount = 0;
        while (true) {
            try {
                return action.call();
            } catch (final Exception e) {
                retryCount++;
                if (retryCount > maxRetries || !retryCondition.getAsBoolean()) {
                    rethrow(e);
                }

                try {
                    Thread.sleep(retryDelay);
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    rethrow(e);
                }
            }
        }
    }

    private static void rethrow(final Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }

        throw new IllegalStateException(e);
    }

}
