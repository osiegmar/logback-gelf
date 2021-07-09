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

import java.io.IOException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public abstract class AbstractGelfAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final int DEFAULT_GELF_PORT = 12201;

    /**
     * IP or hostname of graylog server.
     */
    private String graylogHost;

    /**
     * Port of graylog server. Default: {@value DEFAULT_GELF_PORT}.
     */
    private int graylogPort = DEFAULT_GELF_PORT;

    private GelfEncoder encoder;

    public String getGraylogHost() {
        return graylogHost;
    }

    public void setGraylogHost(final String graylogHost) {
        this.graylogHost = graylogHost;
    }

    public int getGraylogPort() {
        return graylogPort;
    }

    public void setGraylogPort(final int graylogPort) {
        this.graylogPort = graylogPort;
    }

    public GelfEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(final GelfEncoder encoder) {
        this.encoder = encoder;
    }

    @SuppressWarnings("checkstyle:illegalcatch")
    @Override
    public final void start() {
        if (graylogHost == null) {
            addError("No graylogHost configured");
            return;
        }

        if (encoder == null) {
            encoder = new GelfEncoder();
            encoder.setContext(getContext());
            encoder.start();
        }

        if (encoder.isAppendNewline()) {
            addError("Newline separator must not be enabled in layout");
            return;
        }

        try {
            startAppender();

            super.start();
        } catch (final Exception e) {
            addError("Couldn't start appender", e);
        }
    }

    protected abstract void startAppender() throws IOException;

    @SuppressWarnings("checkstyle:illegalcatch")
    @Override
    protected void append(final ILoggingEvent event) {
        final byte[] binMessage = encoder.encode(event);

        try {
            appendMessage(binMessage);
        } catch (final Exception e) {
            // Could be IOException or some kind of RuntimeException
            addError("Error sending GELF message", e);
        }
    }

    protected abstract void appendMessage(byte[] messageToSend) throws IOException;

    @Override
    public void stop() {
        super.stop();
        try {
            close();
        } catch (final IOException e) {
            addError("Couldn't close appender", e);
        }
    }

    protected abstract void close() throws IOException;

}
