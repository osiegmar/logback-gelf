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

package de.siegmar.logbackgelf.pool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class SimpleObjectPoolTest {

    private final PooledObjectFactory<MyPooledObject> factory =
        new PooledObjectFactory<>() {

            private int i = 1;

            @Override
            public MyPooledObject newInstance() {
                return new MyPooledObject(i++);
            }

        };

    @Test
    void simple() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
            new SimpleObjectPool<>(factory, 2, 100, 100, 100);

        for (int i = 0; i < 10; i++) {
            for (int y = 1; y < 3; y++) {
                final MyPooledObject o1 = pool.borrowObject();
                assertThat(o1.getId()).isEqualTo(y);
                pool.returnObject(o1);
            }
        }
    }

    @Test
    void invalidate() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
            new SimpleObjectPool<>(factory, 2, 100, 100, 100);

        for (int i = 1; i < 4; i++) {
            final MyPooledObject o1 = pool.borrowObject();
            assertThat(o1.getId()).isEqualTo(i);
            pool.invalidateObject(o1);
        }
    }

    @Test
    void maxLastBorrowed() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
            new SimpleObjectPool<>(factory, 1, 100, 100, 0);

        for (int i = 1; i < 4; i++) {
            if (i > 1) {
                Thread.sleep(2);
            }

            final MyPooledObject o1 = pool.borrowObject();
            assertThat(o1.getId()).isEqualTo(i);
            pool.returnObject(o1);
        }
    }

    @Test
    void availableObjectsExhausted() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
            new SimpleObjectPool<>(factory, 1, 100, 100, 0);

        final Object o1 = pool.borrowObject();
        assertThat(o1).isNotNull();

        assertThatThrownBy(pool::borrowObject)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Couldn't acquire connection from pool");
    }

    @Test
    void validatePoolSize() {
        assertThatThrownBy(() -> new SimpleObjectPool<>(factory, 0, 100, 100, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("poolSize must be > 0");
    }

    @Test
    void infinitePollWaitTime() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
            new SimpleObjectPool<>(factory, 1, -1, 100, 0);

        final MyPooledObject o1 = pool.borrowObject();
        final long before = System.nanoTime();

        Executors.newScheduledThreadPool(1)
            .schedule(() -> pool.returnObject(o1), 200, TimeUnit.MILLISECONDS);

        final MyPooledObject o2 = pool.borrowObject();
        final long after = System.nanoTime();

        final long elapsedTimeMillis = TimeUnit.NANOSECONDS.toMillis(after - before);

        assertThat(o2).isNotNull();
        assertThat(elapsedTimeMillis).isGreaterThan(200);
    }

    @Test
    void infiniteLifeTime() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
            new SimpleObjectPool<>(factory, 1, 100, -1, -1);

        final MyPooledObject o1 = pool.borrowObject();
        pool.returnObject(o1);

        final MyPooledObject o2 = pool.borrowObject();

        assertThat(o2).isEqualTo(o1);
    }

    @Test
    void recycleByLifeTime() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
            new SimpleObjectPool<>(factory, 1, -1, 0, -1);

        final MyPooledObject o1 = pool.borrowObject();
        pool.returnObject(o1);

        // Force the objects to be recycled in the pool
        Thread.sleep(2);

        final MyPooledObject o2 = pool.borrowObject();

        assertThat(o2).isNotEqualTo(o1);
    }

    private static final class MyPooledObject extends AbstractPooledObject {

        private final int id;

        MyPooledObject(final int id) {
            this.id = id;
        }

        int getId() {
            return id;
        }

    }

}
