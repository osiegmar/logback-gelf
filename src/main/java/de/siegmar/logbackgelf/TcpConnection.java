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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;

import ch.qos.logback.core.util.CloseUtil;
import de.siegmar.logbackgelf.pool.BasePooledObject;

public class TcpConnection extends BasePooledObject {

    private final AddressResolver addressResolver;
    private final SocketFactory socketFactory;
    private final int port;
    private final int connectTimeout;
    private final int socketTimeout;

    private volatile OutputStream outputStream;

    TcpConnection(final SocketFactory socketFactory,
                  final AddressResolver addressResolver, final int port, final int connectTimeout,
                  final int socketTimeout) {

        this.addressResolver = addressResolver;
        this.socketFactory = socketFactory;
        this.port = port;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
    }

    public void write(final byte[] messageToSend) throws IOException {
        if (outputStream == null) {
            connect();
        }

        outputStream.write(messageToSend);

        // GELF via TCP requires 0 termination
        outputStream.write(0);

        outputStream.flush();
    }

    private void connect() throws IOException {
        @SuppressWarnings("PMD.CloseResource")
        final Socket socket = socketFactory.createSocket();

        socket.setSoTimeout(socketTimeout);
        final InetAddress ip = addressResolver.resolve();
        socket.connect(new InetSocketAddress(ip, port), connectTimeout);
        outputStream = socket.getOutputStream();
    }

    @Override
    protected void close() {
        CloseUtil.closeQuietly(outputStream);
    }

}
