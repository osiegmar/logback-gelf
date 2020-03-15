# Logback GELF

[![Build Status](https://api.travis-ci.org/osiegmar/logback-gelf.svg)](https://travis-ci.org/osiegmar/logback-gelf)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.siegmar/logback-gelf/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.siegmar/logback-gelf)

Logback appender for sending GELF (Graylog Extended Log Format) messages with zero additional
dependencies.


## Features

- UDP (with chunking)
- TCP (with or without TLS encryption)
- Deflate compression in UDP mode
- Client side load balancing (round robin)
- Forwarding of MDC (Mapped Diagnostic Context)
- Forwarding of caller data
- Forwarding of static fields
- Forwarding of exception root cause
- No runtime dependencies beside Logback


## Requirements

- Java 8
- Logback 1.2.3


## Examples

Simple UDP configuration:

```xml
<configuration>

    <appender name="GELF" class="de.siegmar.logbackgelf.GelfUdpAppender">
        <graylogHost>localhost</graylogHost>
        <graylogPort>12201</graylogPort>
    </appender>

    <root level="debug">
        <appender-ref ref="GELF" />
    </root>

</configuration>
```

Simple TCP configuration:

```xml
<configuration>

    <appender name="GELF" class="de.siegmar.logbackgelf.GelfTcpAppender">
        <graylogHost>localhost</graylogHost>
        <graylogPort>12201</graylogPort>
    </appender>

    <!-- Use AsyncAppender to prevent slowdowns -->
    <appender name="ASYNC GELF" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="GELF" />
    </appender>

    <root level="debug">
        <appender-ref ref="ASYNC GELF" />
    </root>

</configuration>
```

Simple TCP with TLS configuration:

```xml
<configuration>

    <appender name="GELF" class="de.siegmar.logbackgelf.GelfTcpTlsAppender">
        <graylogHost>localhost</graylogHost>
        <graylogPort>12201</graylogPort>
    </appender>

    <!-- Use AsyncAppender to prevent slowdowns -->
    <appender name="ASYNC GELF" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="GELF" />
    </appender>

    <root level="debug">
        <appender-ref ref="ASYNC GELF" />
    </root>

</configuration>
```

Find more advanced examples in the [examples directory](examples).


## Configuration

### Appender

`de.siegmar.logbackgelf.GelfUdpAppender`

* **graylogHost**: IP or hostname of graylog server.
  If the hostname resolves to multiple ip addresses, round robin will be used.
* **graylogPort**: Port of graylog server. Default: 12201.
* **encoder**: See Encoder configuration below.
* **maxChunkSize**: Maximum size of GELF chunks in bytes. Default chunk size is 508 - this prevents
  IP packet fragmentation. This is also the recommended minimum.
  Maximum supported chunk size is 65,467 bytes.
* **useCompression**: If true, compression of GELF messages is enabled. Default: true.
* **messageIdSupplier**: The mechanism that supplies unique message ids that are required by the
  GELF UDP protocol. Default: `de.siegmar.logbackgelf.MessageIdSupplier`.


`de.siegmar.logbackgelf.GelfTcpAppender`

* **graylogHost**: IP or hostname of graylog server.
  If the hostname resolves to multiple ip addresses, round robin will be used.
* **graylogPort**: Port of graylog server. Default: 12201.
* **encoder**: See Encoder configuration below.
* **connectTimeout**: Maximum time (in milliseconds) to wait for establishing a connection. A value
  of 0 disables the connect timeout. Default: 15,000 milliseconds.
* **reconnectInterval**: Time interval (in seconds) after an existing connection is closed and
  re-opened. A value of -1 disables automatic reconnects. Default: 60 seconds.
* **maxRetries**: Number of retries. A value of 0 disables retry attempts. Default: 2.
* **retryDelay**: Time (in milliseconds) between retry attempts. Ignored if maxRetries is 0.
  Default: 3,000 milliseconds.
* **poolSize**: Number of concurrent tcp connections (minimum 1). Default: 2.
* **poolMaxWaitTime**: Maximum amount of time (in milliseconds) to wait for a connection to become
  available from the pool. A value of -1 disables the timeout. Default: 5,000 milliseconds.


`de.siegmar.logbackgelf.GelfTcpTlsAppender`

* Everything from GelfTcpAppender
* **insecure**: If true, skip the TLS certificate validation.
  You should not use this in production! Default: false.


### Encoder

`de.siegmar.logbackgelf.GelfEncoder`

* **originHost**: Origin hostname - will be auto detected if not specified.
* **includeRawMessage**: If true, the raw message (with argument placeholders) will be sent, too.
  Default: false.
* **includeMarker**: If true, logback markers will be sent, too. Default: true.
* **includeMdcData**: If true, MDC keys/values will be sent, too. Default: true.
* **includeCallerData**: If true, caller data (source file-, method-, class name and line) will be
  sent, too. Default: false.
* **includeRootCauseData**: If true, root cause exception of the exception passed with the log
   message will be exposed in the root_cause_class_name and root_cause_message fields.
   Default: false.
* **includeLevelName**: If true, the log level name (e.g. DEBUG) will be sent, too. Default: false.
* **levelNameKey**: The key (i.e. the field name) that should be used for the log level name. 
  This is only relevant when includeLevelName is true. Default: level_name.
* **loggerNameKey**: The key (i.e. the field name) that should be used for the logger name. 
  Default: logger_name.
* **threadNameKey**: The key (i.e. the field name) that should be used for the thread name. 
  Default: thread_name.
* **appendNewline**: If true, a system depended newline separator will be added at the end of each message.
  Don't use this in conjunction with TCP or UDP appenders, as this is only reasonable for
  console logging!
* **shortPatternLayout**: Short message format. Default: `"%m%nopex"`.
* **fullPatternLayout**: Full message format (Stacktrace). Default: `"%m%n"`.
* **numbersAsString**: Log numbers as String. Default: false.
* **staticFields**: Additional, static fields to send to graylog. Defaults: none.


## Troubleshooting

If you have any problems, enable the debug mode and check the logs.

```xml
<configuration debug="true">
    ...
</configuration>
```


## Contribution

- Fork
- Code
- Add test(s)
- Commit
- Send me a pull request


## Copyright

Copyright (C) 2016-2020 Oliver Siegmar

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
