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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SimpleObjectPool<T extends AbstractPooledObject> {

    private static final int MILLIS_PER_SECOND = 1000;

    private final BlockingQueue<T> pool = new LinkedBlockingQueue<>();
    private final Set<T> allObjects = new HashSet<>();

    private final PooledObjectFactory<T> objectFactory;
    private final int maxWaitTime;
    private final int maxLifeTime;
    private final int maxIdleTime;

    public SimpleObjectPool(final PooledObjectFactory<T> objectFactory,
                            final int poolSize, final int maxWaitTime,
                            final int maxLifeTime, final int maxIdleTime) {

        if (poolSize < 1) {
            throw new IllegalArgumentException("poolSize must be > 0");
        }

        this.objectFactory = objectFactory;
        this.maxWaitTime = maxWaitTime;
        this.maxLifeTime = maxLifeTime < 0 ? maxLifeTime : maxLifeTime * MILLIS_PER_SECOND;
        this.maxIdleTime = maxIdleTime < 0 ? maxIdleTime : maxIdleTime * MILLIS_PER_SECOND;

        for (int i = 0; i < poolSize; i++) {
            final T pooledObject = this.objectFactory.newInstance();
            pool.add(pooledObject);
            allObjects.add(pooledObject);
        }
    }

    @SuppressWarnings("checkstyle:illegalcatch")
    public void execute(final PooledObjectConsumer<T> consumer) throws Exception {
        T pooledObject = null;
        try {
            pooledObject = borrowObject();
            consumer.accept(pooledObject);
        } catch (final Exception e) {
            if (pooledObject != null) {
                invalidateObject(pooledObject);
                pooledObject = null;
            }

            throw e;
        } finally {
            if (pooledObject != null) {
                returnObject(pooledObject);
            }
        }
    }

    public T borrowObject() throws InterruptedException {
        final T pooledObject;
        if (maxWaitTime < 0) {
            pooledObject = pool.take();
        } else {
            pooledObject = pool.poll(maxWaitTime, TimeUnit.MILLISECONDS);

            if (pooledObject == null) {
                throw new IllegalStateException("Couldn't acquire connection from pool");
            }
        }

        final T result = needToEvict(pooledObject) ? recycle(pooledObject) : pooledObject;
        result.borrow();
        return result;
    }

    private boolean needToEvict(final T pooledObject) {
        if (maxLifeTime > 0 && pooledObject.lifeTime() > maxLifeTime) {
            return true;
        } else if (maxIdleTime >= 0 && (System.currentTimeMillis() - pooledObject.lastBorrowed()) > maxIdleTime) {
            return true;
        } else {
            return false;
        }
    }

    private T recycle(final T oldInstance) {
        final T newInstance = objectFactory.newInstance();
        synchronized (allObjects) {
            allObjects.remove(oldInstance);
            allObjects.add(newInstance);
        }

        oldInstance.close();
        return newInstance;
    }

    public void returnObject(final T pooledObject) {
        pool.add(pooledObject);
    }

    public void invalidateObject(final T pooledObject) {
        pool.add(recycle(pooledObject));
    }

    public void close() {
        synchronized (allObjects) {
            for (T object : allObjects) {
                object.close();
            }
        }
    }

}
