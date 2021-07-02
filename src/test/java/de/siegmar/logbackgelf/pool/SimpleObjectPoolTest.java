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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class SimpleObjectPoolTest {

    private final PooledObjectFactory<MyPooledObject> factory =
        new PooledObjectFactory<MyPooledObject>() {

            private int i = 1;

            @Override
            public MyPooledObject newInstance() {
                return new MyPooledObject(i++);
            }

        };

    @Test
    public void simple() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
            new SimpleObjectPool<>(factory, 2, 100, 100, 100);

        for (int i = 0; i < 10; i++) {
            final MyPooledObject o1 = pool.borrowObject();
            assertEquals(1, o1.getId());
            pool.returnObject(o1);

            final MyPooledObject o2 = pool.borrowObject();
            assertEquals(2, o2.getId());
            pool.returnObject(o2);
        }
    }

    @Test
    public void invalidate() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
            new SimpleObjectPool<>(factory, 2, 100, 100, 100);

        final MyPooledObject o1 = pool.borrowObject();
        assertEquals(1, o1.getId());
        pool.invalidateObject(o1);

        final MyPooledObject o2 = pool.borrowObject();
        assertEquals(2, o2.getId());
        pool.returnObject(o2);

        final MyPooledObject o3 = pool.borrowObject();
        assertEquals(3, o3.getId());
        pool.returnObject(o3);
    }

    @Test
    public void maxLastBorrowed() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
                new SimpleObjectPool<>(factory, 1, 100, 100, 0);

        final MyPooledObject o1 = pool.borrowObject();
        assertEquals(1, o1.getId());
        pool.returnObject(o1);

        Thread.sleep(2);

        final MyPooledObject o2 = pool.borrowObject();
        assertEquals(2, o2.getId());
        pool.returnObject(o2);

        Thread.sleep(2);

        final MyPooledObject o3 = pool.borrowObject();
        assertEquals(3, o3.getId());
        pool.returnObject(o3);
    }

    @Test
    void availableObjectsExhausted() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
                new SimpleObjectPool<>(factory, 1, 100, 100, 0);

        final Object o1 = pool.borrowObject();
        assertNotNull(o1);

        assertThrows(IllegalStateException.class, pool::borrowObject,
                "Couldn't acquire connection from pool");
    }

    @Test
    void validatePoolSize() {
        assertThrows(IllegalArgumentException.class, () ->
                new SimpleObjectPool<>(factory, 0, 100, 100, 0),
                "poolSize must be > 0");
    }

    @Test
    void infinitePollWaitTime() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
                new SimpleObjectPool<>(factory, 1, -1, 100, 0);

        final MyPooledObject o1 = pool.borrowObject();
        final long before = System.currentTimeMillis();

        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            pool.returnObject(o1);
        }).start();

        final MyPooledObject o2 = pool.borrowObject();
        final long after = System.currentTimeMillis();

        assertNotNull(o2);
        assertTrue(after - before > 190);
    }

    @Test
    void infiniteLifeTime() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
                new SimpleObjectPool<>(factory, 1, 100, -1, -1);

        final MyPooledObject o1 = pool.borrowObject();
        pool.returnObject(o1);

        Thread.sleep(200);

        final MyPooledObject o2 = pool.borrowObject();

        assertEquals(o1, o2);
    }

    @Test
    void recycleByLifeTime() throws InterruptedException {
        final SimpleObjectPool<MyPooledObject> pool =
                new SimpleObjectPool<>(factory, 1, 100, 0, 100);

        final MyPooledObject o1 = pool.borrowObject();
        pool.returnObject(o1);

        Thread.sleep(2);

        final MyPooledObject o2 = pool.borrowObject();

        assertNotEquals(o1, o2);
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
