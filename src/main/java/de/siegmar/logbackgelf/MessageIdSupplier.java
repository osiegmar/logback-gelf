/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2020 Oliver Siegmar
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

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/**
 * Supplier implementation for GELF message IDs as used for UDP chunks. Unfortunately the GELF
 * protocol limits the message id length to 8 bytes thus a UUID cannot be used (16 bytes).
 */
public class MessageIdSupplier implements LongSupplier {

    private static final long BITS_13 = 0b1_1111_1111_1111;

    @SuppressWarnings("checkstyle:magicnumber")
    @Override
    public long getAsLong() {
        /*
         * Idea is borrowed from logstash-gelf by <a href="https://github.com/mp911de">Mark Paluch</a>, MIT licensed
         * <a href="https://github.com/mp911de/logstash-gelf/blob/a938063de1f822c8d26c8d51ed3871db24355017/src/main/java/biz/paluch/logging/gelf/intern/GelfMessage.java">GelfMessage.java</a>
         *
         * Considerations about generating the message ID: The GELF documentation suggests to
         * "Generate from millisecond timestamp + hostname, for example.":
         * https://go2docs.graylog.org/5-1/getting_in_log_data/gelf.html#GELFviaUDP
         *
         * However, relying on current time in milliseconds on the same system will result in a high collision
         * probability if lots of messages are generated quickly. Things will be even worse if multiple servers send
         * to the same log server. Adding the hostname is not guaranteed to help, and if the hostname is the FQDN it
         * is even unlikely to be unique at all.
         *
         * The GELF module used by Logstash uses the first eight bytes of an MD5 hash of the current time as floating
         * point, a hyphen, and an eight byte random number: https://github.com/logstash-plugins/logstash-output-gelf
         * https://github.com/graylog-labs/gelf-rb/blob/master/lib/gelf/notifier.rb#L239 It probably doesn't have to
         * be that clever:
         *
         * Using the timestamp plus a random number will mean we only have to worry about collision of random numbers
         * within the same milliseconds. How short can the timestamp be before it will collide with old timestamps?
         * Every second Graylog will evict expired messaged (5 seconds old) from the pool:
         * https://github.com/Graylog2/graylog2-server/blob/master/graylog2-server/src/main/java/org/graylog2/inputs/codecs/
         * GelfChunkAggregator.java Thus, we just need six seconds which will require 13 bits.
         * Then we can spend the rest on a random number.
         */

        return (random() & ~BITS_13) | (currentTime() & BITS_13);
    }

    long random() {
        return ThreadLocalRandom.current().nextLong();
    }

    long currentTime() {
        return System.currentTimeMillis();
    }

}
