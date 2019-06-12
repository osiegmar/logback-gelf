Logback GELF
============

[![Build Status](https://api.travis-ci.org/osiegmar/logback-gelf.svg)](https://travis-ci.org/osiegmar/logback-gelf)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.siegmar/logback-gelf/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.siegmar/logback-gelf)

Logback appender for sending GELF (Graylog Extended Log Format) messages with zero additional
dependencies.


Latest release
--------------

The most recent release is 2.0.1, released May 19, 2019.

To add a dependency using Maven, use the following:

```xml
<dependency>
    <groupId>de.siegmar</groupId>
    <artifactId>logback-gelf</artifactId>
    <version>2.0.1</version>
</dependency>
```

To add a dependency using Gradle:

```gradle
dependencies {
    compile 'de.siegmar:logback-gelf:2.0.1'
}
```


Features
--------

- UDP (with chunking)
- TCP (with or without TLS encryption)
- Deflate compression in UDP mode
- Client side load balancing (round robin)
- Forwarding of MDC (Mapped Diagnostic Context)
- Forwarding of caller data
- Forwarding of static fields
- Forwarding of exception root cause
- No runtime dependencies beside Logback


Requirements
------------

- Java 7
- Logback 1.2.3


Important notes
---------------

Version 2.0.0 of this library introduced a configuration change. If you were already using this library, update
your configuration to keep it working!

**Old** format:
```xml
<layout class="de.siegmar.logbackgelf.GelfLayout">
```

**New** format:
```xml
<encoder class="de.siegmar.logbackgelf.GelfEncoder">
```

This change was introduced, as the API of the Encoder interface changed in Logback 1.2.0.


Example
-------

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

    <root level="debug">
        <appender-ref ref="GELF" />
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

    <root level="debug">
        <appender-ref ref="GELF" />
    </root>

</configuration>
```

**Please note, that it is recommended to use Logback's AsyncAppender in conjunction with
GelfTcpAppender or GelfTcpTlsAppender to send logs asynchronously.
See the advanced configuration example below.**


Advanced UDP configuration:

```xml
<configuration>

    <appender name="GELF" class="de.siegmar.logbackgelf.GelfUdpAppender">
        <graylogHost>localhost</graylogHost>
        <graylogPort>12201</graylogPort>
        <maxChunkSize>508</maxChunkSize>
        <useCompression>true</useCompression>
        <encoder class="de.siegmar.logbackgelf.GelfEncoder">
            <originHost>localhost</originHost>
            <includeRawMessage>false</includeRawMessage>
            <includeMarker>true</includeMarker>
            <includeMdcData>true</includeMdcData>
            <includeCallerData>false</includeCallerData>
            <includeRootCauseData>false</includeRootCauseData>
            <includeLevelName>false</includeLevelName>
            <shortPatternLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m%nopex</pattern>
            </shortPatternLayout>
            <fullPatternLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m%n</pattern>
            </fullPatternLayout>
            <numbersAsString>false</numbersAsString>
            <staticField>app_name:backend</staticField>
            <staticField>os_arch:${os.arch}</staticField>
            <staticField>os_name:${os.name}</staticField>
            <staticField>os_version:${os.version}</staticField>
        </encoder>
    </appender>

    <root level="debug">
        <appender-ref ref="GELF" />
    </root>

</configuration>
```

Advanced TCP configuration:

```xml
<configuration>

    <appender name="GELF" class="de.siegmar.logbackgelf.GelfTcpAppender">
        <graylogHost>localhost</graylogHost>
        <graylogPort>12201</graylogPort>
        <connectTimeout>15000</connectTimeout>
        <reconnectInterval>300</reconnectInterval>
        <maxRetries>2</maxRetries>
        <retryDelay>3000</retryDelay>
        <poolSize>2</poolSize>
        <poolMaxWaitTime>5000</poolMaxWaitTime>
        <encoder class="de.siegmar.logbackgelf.GelfEncoder">
            <originHost>localhost</originHost>
            <includeRawMessage>false</includeRawMessage>
            <includeMarker>true</includeMarker>
            <includeMdcData>true</includeMdcData>
            <includeCallerData>false</includeCallerData>
            <includeRootCauseData>false</includeRootCauseData>
            <includeLevelName>false</includeLevelName>
            <shortPatternLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m%nopex</pattern>
            </shortPatternLayout>
            <fullPatternLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m%n</pattern>
            </fullPatternLayout>
            <numbersAsString>false</numbersAsString>
            <staticField>app_name:backend</staticField>
            <staticField>os_arch:${os.arch}</staticField>
            <staticField>os_name:${os.name}</staticField>
            <staticField>os_version:${os.version}</staticField>
        </encoder>
    </appender>

    <appender name="ASYNC GELF" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="GELF" />
    </appender>

    <root level="debug">
        <appender-ref ref="ASYNC GELF" />
    </root>

</configuration>
```

Advanced TCP with TLS configuration:

```xml
<configuration>

    <appender name="GELF" class="de.siegmar.logbackgelf.GelfTcpTlsAppender">
        <graylogHost>localhost</graylogHost>
        <graylogPort>12201</graylogPort>
        <connectTimeout>15000</connectTimeout>
        <reconnectInterval>300</reconnectInterval>
        <maxRetries>2</maxRetries>
        <retryDelay>3000</retryDelay>
        <poolSize>2</poolSize>
        <poolMaxWaitTime>5000</poolMaxWaitTime>
        <trustAllCertificates>false</trustAllCertificates>
        <encoder class="de.siegmar.logbackgelf.GelfEncoder">
            <originHost>localhost</originHost>
            <includeRawMessage>false</includeRawMessage>
            <includeMarker>true</includeMarker>
            <includeMdcData>true</includeMdcData>
            <includeCallerData>false</includeCallerData>
            <includeRootCauseData>false</includeRootCauseData>
            <includeLevelName>false</includeLevelName>
            <shortPatternLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m%nopex</pattern>
            </shortPatternLayout>
            <fullPatternLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m%n</pattern>
            </fullPatternLayout>
            <numbersAsString>false</numbersAsString>
            <staticField>app_name:backend</staticField>
            <staticField>os_arch:${os.arch}</staticField>
            <staticField>os_name:${os.name}</staticField>
            <staticField>os_version:${os.version}</staticField>
        </encoder>
    </appender>

    <appender name="ASYNC GELF" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="GELF" />
    </appender>

    <root level="debug">
        <appender-ref ref="ASYNC GELF" />
    </root>

</configuration>
```

Configuration
-------------

## Appender

`de.siegmar.logbackgelf.GelfUdpAppender`

* **graylogHost**: IP or hostname of graylog server.
  If the hostname resolves to multiple ip addresses, round robin will be used.
* **graylogPort**: Port of graylog server. Default: 12201.
* **encoder**: See Encoder configuration below.
* **maxChunkSize**: Maximum size of GELF chunks in bytes. Default chunk size is 508 - this prevents
  IP packet fragmentation. This is also the recommended minimum.
  Maximum supported chunk size is 65,467 bytes.
* **useCompression**: If true, compression of GELF messages is enabled. Default: true.


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
* **trustAllCertificates**: If true, trust all TLS certificates (even self signed certificates).
  You should not use this in production! Default: false.

## Encoder

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
  This is only relevant when includeLevelName is trueö. Default: level_name.
* **appendNewline**: If true, a system depended newline separator will be added at the end of each message.
  Don't use this in conjunction with TCP or UDP appenders, as this is only reasonable for
  console logging!
* **shortPatternLayout**: Short message format. Default: `"%m%nopex"`.
* **fullPatternLayout**: Full message format (Stacktrace). Default: `"%m%n"`.
* **numbersAsString**: Log numbers as String. Default: true (will be changed in next major release).
* **staticFields**: Additional, static fields to send to graylog. Defaults: none.


Contribution
------------

- Fork
- Code
- Add test(s)
- Commit
- Send me a pull request


Copyright
---------

Copyright (C) 2016-2019 Oliver Siegmar

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
