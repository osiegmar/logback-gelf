/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2016 Oliver Siegmar
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

import java.util.Arrays;

import javax.net.SocketFactory;

import de.siegmar.logbackgelf.pool.SimpleObjectPool;

public class GelfTcpAppender extends AbstractGelfAppender {

    private static final int DEFAULT_CONNECT_TIMEOUT = 15_000;
    private static final int DEFAULT_RECONNECT_INTERVAL = 60;
    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final int DEFAULT_RETRY_DELAY = 3_000;
    private static final int DEFAULT_POOL_SIZE = 2;
    private static final int DEFAULT_POOL_MAX_WAIT_TIME = 5_000;

    /**
     * Maximum time (in milliseconds) to wait for establishing a connection. A value of 0 disables
     * the connect timeout. Default: 15,000 milliseconds.
     */
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    /**
     * Time interval (in seconds) after an existing connection is closed and re-opened.
     * A value of -1 disables automatic reconnects. Default: 60 seconds.
     */
    private int reconnectInterval = DEFAULT_RECONNECT_INTERVAL;

    /**
     * Number of retries. A value of 0 disables retry attempts. Default: 2.
     */
    private int maxRetries = DEFAULT_MAX_RETRIES;

    /**
     * Time (in milliseconds) between retry attempts. Ignored if maxRetries is 0.
     * Default: 3,000 milliseconds.
     */
    private int retryDelay = DEFAULT_RETRY_DELAY;

    /**
     * Number of concurrent tcp connections (minimum 1). Default: 2.
     */
    private int poolSize = DEFAULT_POOL_SIZE;

    /**
     * Maximum amount of time (in milliseconds) to wait for a connection to become
     * available from the pool. A value of -1 disables the timeout. Default: 5,000 milliseconds.
     */
    private int poolMaxWaitTime = DEFAULT_POOL_MAX_WAIT_TIME;

    private SimpleObjectPool<TcpConnection> connectionPool;

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(final int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(final int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(final int retryDelay) {
        this.retryDelay = retryDelay;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(final int poolSize) {
        this.poolSize = poolSize;
    }

    public long getPoolMaxWaitTime() {
        return poolMaxWaitTime;
    }

    public void setPoolMaxWaitTime(final int poolMaxWaitTime) {
        this.poolMaxWaitTime = poolMaxWaitTime;
    }

    protected void startAppender() {
        final AddressResolver addressResolver = new AddressResolver(getGraylogHost());

        connectionPool = new SimpleObjectPool<>(() -> new TcpConnection(initSocketFactory(),
            addressResolver, getGraylogPort(), connectTimeout),
            poolSize, poolMaxWaitTime, reconnectInterval);
    }

    protected SocketFactory initSocketFactory() {
        return SocketFactory.getDefault();
    }

    @Override
    protected void appendMessage(final byte[] messageToSend) {
        // GELF via TCP requires 0 termination
        final byte[] tcpMessage = Arrays.copyOf(messageToSend, messageToSend.length + 1);

        int openRetries = maxRetries;
        do {
            if (sendMessage(tcpMessage)) {
                // Message was sent successfully - we're done with it
                break;
            }

            if (retryDelay > 0 && openRetries > 0) {
                try {
                    Thread.sleep(retryDelay);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } while (openRetries-- > 0 && isStarted());
    }

    /**
     * Send message to socket's output stream.
     *
     * @param messageToSend message to send.
     *
     * @return {@code true} if message was sent successfully, {@code false} otherwise.
     */
    @SuppressWarnings("checkstyle:illegalcatch")
    private boolean sendMessage(final byte[] messageToSend) {
        try {
            connectionPool.execute(tcpConnection -> tcpConnection.write(messageToSend));
        } catch (final Exception e) {
            addError(String.format("Error sending message via tcp://%s:%s",
                getGraylogHost(), getGraylogPort()), e);

            return false;
        }

        return true;
    }

    @Override
    protected void close() {
        connectionPool.close();
    }

}
