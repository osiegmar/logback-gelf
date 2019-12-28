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
            new SimpleObjectPool<>(factory, 2, 100, 100);

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
            new SimpleObjectPool<>(factory, 2, 100, 100);

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
