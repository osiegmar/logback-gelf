# Logback GELF

[![build](https://github.com/osiegmar/logback-gelf/actions/workflows/build.yml/badge.svg)](https://github.com/osiegmar/logback-gelf/actions/workflows/build.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/975049eb1352478a89bb6d2e9d43e2be)](https://app.codacy.com/gh/osiegmar/logback-gelf/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![codecov](https://codecov.io/gh/osiegmar/logback-gelf/graph/badge.svg?token=YfDHBxprtb)](https://codecov.io/gh/osiegmar/logback-gelf)
[![javadoc](https://javadoc.io/badge2/de.siegmar/logback-gelf/javadoc.svg)](https://javadoc.io/doc/de.siegmar/logback-gelf)
[![Maven Central](https://img.shields.io/maven-central/v/de.siegmar/logback-gelf.svg)](https://central.sonatype.com/artifact/de.siegmar/logback-gelf)

Logback appender for sending GELF (Graylog Extended Log Format) messages with zero additional
dependencies.

## Features

- UDP (with chunking)
- TCP (with or without TLS encryption)
- HTTP(s)
- GZIP and ZLIB compression (in UDP and HTTP mode)
- Client side load balancing (round-robin)
- Forwarding of MDC (Mapped Diagnostic Context)
- Forwarding of caller data
- Forwarding of static fields
- Forwarding of exception root cause
- No runtime dependencies beside Logback

## Requirements

- Java 11
- Logback 1.5.15

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
        <neverBlock>true</neverBlock>
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
        <neverBlock>true</neverBlock>
    </appender>

    <root level="debug">
        <appender-ref ref="ASYNC GELF" />
    </root>

</configuration>
```

Simple HTTP configuration:

```xml
<configuration>

    <appender name="GELF" class="de.siegmar.logbackgelf.GelfHttpAppender">
        <uri>https://my.server:12201/gelf</uri>
    </appender>

    <!-- Use AsyncAppender to prevent slowdowns -->
    <appender name="ASYNC GELF" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="GELF" />
        <neverBlock>true</neverBlock>
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
  If the hostname resolves to multiple ip addresses, round-robin will be used.
* **graylogPort**: Port of graylog server. Default: 12201.
* **maxChunkSize**: Maximum size of GELF chunks in bytes. Default chunk size is 508 - this prevents
  IP packet fragmentation. This is also the recommended minimum.
  Maximum supported chunk size is 65,467 bytes.
* **compressionMethod**: Compression method to use (NONE, GZIP or ZLIB). Default: GZIP.
* **messageIdSupplier**: The mechanism that supplies unique message ids that are required by the
  GELF UDP protocol. Default: `de.siegmar.logbackgelf.MessageIdSupplier`.
* **encoder**: See Encoder configuration below.

`de.siegmar.logbackgelf.GelfTcpAppender`

* **graylogHost**: IP or hostname of graylog server.
  If the hostname resolves to multiple ip addresses, round-robin will be used.
* **graylogPort**: Port of graylog server. Default: 12201.
* **connectTimeout**: Maximum time (in milliseconds) to wait for establishing a connection. A value
  of 0 disables the connect timeout. Default: 15,000 milliseconds.
* **socketTimeout**: Maximum time (in milliseconds) to block when reading a socket. A value of 0 disables
  the socket timeout. Default: 5,000 milliseconds.
* **reconnectInterval**: Time interval (in seconds) after an existing connection is closed and
  re-opened. A value of -1 disables automatic reconnects. Default: 60 seconds.
* **maxRetries**: Number of retries. A value of 0 disables retry attempts. Default: 2.
* **retryDelay**: Time (in milliseconds) between retry attempts. Ignored if maxRetries is 0.
  Default: 3,000 milliseconds.
* **poolSize**: Number of concurrent tcp connections (minimum 1). Default: 2.
* **poolMaxWaitTime**: Maximum amount of time (in milliseconds) to wait for a connection to become
  available from the pool. A value of -1 disables the timeout. Default: 5,000 milliseconds.
* **poolMaxIdleTime**: Maximum amount of time (in seconds) that a pooled connection can be idle
  before it is considered 'stale' and will not be reused. A value of -1 disables the max idle time
  feature. Default: -1 (disabled).
* **encoder**: See Encoder configuration below.

`de.siegmar.logbackgelf.GelfTcpTlsAppender`

* Everything from GelfTcpAppender
* **insecure**: If true, skip the TLS certificate validation.
  You should not use this in production! Default: false.

`de.siegmar.logbackgelf.GelfHttpAppender`

* **uri**: HTTP(s) URI of graylog server (e.g. https://my.server:12201/gelf).
* **insecure**: If true, skip the TLS certificate validation.
  You should not use this in production! Default: false.
* **connectTimeout**: Maximum time (in milliseconds) to wait for establishing a connection. A value
  of 0 disables the connect timeout. Default: 15,000 milliseconds.
* **requestTimeout**: Maximum time (in milliseconds) to wait for a response. A value of 0 disables the timeout.
  Default: 5,000 milliseconds.
* **maxRetries**: Number of retries. A value of 0 disables retry attempts. Default: 2.
* **retryDelay**: Time (in milliseconds) between retry attempts. Ignored if maxRetries is 0.
  Default: 3,000 milliseconds.
* **compressionMethod**: Compression method to use (NONE, GZIP or ZLIB). Default: GZIP.
* **encoder**: See Encoder configuration below.

### Encoder

`de.siegmar.logbackgelf.GelfEncoder`

* **originHost**: Origin hostname - will be auto-detected if not specified.
* **includeRawMessage**: If true, the raw message (with argument placeholders) will be sent, too.
  Default: false.
* **includeKeyValues**: If true, key value pairs will be sent, too. Default: true.
* **includeMarker**: If true, logback markers will be sent, too. Default: false.
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
* **appendNewline**: If true, a system-dependent newline separator will be added at the end of each message.
  Don't use this in conjunction with TCP/UDP/HTTP appender, as this is only reasonable for
  console logging!
* **shortMessageLayout**: Short message format. Default: `"%m%nopex"`.
* **fullMessageLayout**: Full message format (Stacktrace). Default: `"%m%n"`.
* **numbersAsString**: Log numbers as String. Default: false.
* **staticFields**: Additional, static fields to send to graylog. Defaults: none.

## Troubleshooting

If you have any problems, enable the debug mode and check the logs.

```xml
<configuration debug="true">
    ...
</configuration>
```
