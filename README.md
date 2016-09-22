Logback GELF
============

[![Build Status](https://api.travis-ci.org/osiegmar/logback-gelf.svg)](https://travis-ci.org/osiegmar/logback-gelf)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.siegmar/logback-gelf/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.siegmar/logback-gelf)

Logback appender for sending GELF (Graylog Extended Log Format) messages with zero additional
dependencies.


Latest release
--------------

The most recent release is 1.0.0, released March 20, 2016.

To add a dependency using Maven, use the following:

```xml
<dependency>
    <groupId>de.siegmar</groupId>
    <artifactId>logback-gelf</artifactId>
    <version>1.0.0</version>
</dependency>
```

To add a dependency using Gradle:

```gradle
dependencies {
    compile 'de.siegmar:logback-gelf:1.0.0'
}
```


Features
--------

- UDP (with chunking)
- TCP
- Deflate compression in UDP mode
- Forwarding of MDC (Mapped Diagnostic Context)
- Forwarding of caller data
- Forwarding of static fields
- No runtime dependencies beside Logback


Requirements
------------

- Java 7
- Logback 1.1.3 (or later)


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

**Please note, that it is recommended to use Logback's AsyncAppender in conjunction with
GelfTcpAppender to send logs asynchronously. See the advanced configuration example below.**


Advanced UDP configuration:

```xml
<configuration>

    <appender name="GELF" class="de.siegmar.logbackgelf.GelfUdpAppender">
        <graylogHost>localhost</graylogHost>
        <graylogPort>12201</graylogPort>
        <maxChunkSize>508</maxChunkSize>
        <useCompression>true</useCompression>
        <layout class="de.siegmar.logbackgelf.GelfLayout">
            <originHost>localhost</originHost>
            <includeRawMessage>false</includeRawMessage>
            <includeMarker>true</includeMarker>
            <includeMdcData>true</includeMdcData>
            <includeCallerData>false</includeCallerData>
            <includeRootException>false</includeRootException>
            <includeLevelName>false</includeLevelName>
            <includeRootException>false</includeRootException>
            <shortPatternLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m%nopex</pattern>
            </shortPatternLayout>
            <fullPatternLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m</pattern>
            </fullPatternLayout>
            <staticField>app_name:backend</staticField>
            <staticField>os_arch:${os.arch}</staticField>
            <staticField>os_name:${os.name}</staticField>
            <staticField>os_version:${os.version}</staticField>
        </layout>
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
        <layout class="de.siegmar.logbackgelf.GelfLayout">
            <originHost>localhost</originHost>
            <includeRawMessage>false</includeRawMessage>
            <includeMarker>true</includeMarker>
            <includeMdcData>true</includeMdcData>
            <includeCallerData>false</includeCallerData>
            <includeRootException>false</includeRootException>
            <includeLevelName>false</includeLevelName>
            <includeRootException>false</includeRootException>
            <shortPatternLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m%nopex</pattern>
            </shortPatternLayout>
            <fullPatternLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m</pattern>
            </fullPatternLayout>
            <staticField>app_name:backend</staticField>
            <staticField>os_arch:${os.arch}</staticField>
            <staticField>os_name:${os.name}</staticField>
            <staticField>os_version:${os.version}</staticField>
        </layout>
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
* **graylogPort**: Port of graylog server. Default: 12201.
* **layout**: See Layout configuration below.
* **maxChunkSize**: Maximum size of GELF chunks in bytes. Default chunk size is 508 - this prevents
  IP packet fragmentation. This is also the recommended minimum.
  Maximum supported chunk size is 65,467 bytes.
* **useCompression**: If true, compression of GELF messages is enabled. Default: true.


`de.siegmar.logbackgelf.GelfTcpAppender`

* **graylogHost**: IP or hostname of graylog server.
* **graylogPort**: Port of graylog server. Default: 12201.
* **layout**: See Layout configuration below.
* **connectTimeout**: Maximum time (in milliseconds) to wait for establishing a connection. A value
  of 0 disables the connect timeout. Default: 15,000 milliseconds.
* **reconnectInterval**: Time interval (in seconds) after an existing connection is closed and
  re-opened. A value of 0 disables automatic reconnects. Default: 300 seconds.
* **maxRetries**: Number of retries. A value of 0 disables retry attempts. Default: 2.
* **retryDelay**: Time (in milliseconds) between retry attempts. Ignored if maxRetries is 0. 
  Default: 3,000 milliseconds.


## Layout

`de.siegmar.logbackgelf.GelfLayout`

* **originHost**: Origin hostname - will be auto detected if not specified.
* **includeRawMessage**: If true, the raw message (with argument placeholders) will be sent, too. 
  Default: false.
* **includeMarker**: If true, logback markers will be sent, too. Default: true.
* **includeMdcData**: If true, MDC keys/values will be sent, too. Default: true.
* **includeCallerData**: If true, caller data (source file-, method-, class name and line) will be 
  sent, too. Default: false.
* **includeRootException**: If true, root cause exception of the exception passed with the log
   message will be exposed in the exception field. Default: false.
* **includeLevelName**: If true, the log level name (e.g. DEBUG) will be sent, too. Default: false.
* **includeRootException**: If true, the root cause exception class name will be sent, too. Default: false
* **shortPatternLayout**: Short message format. Default: `"%m%nopex"`. 
* **fullPatternLayout**: Full message format (Stacktrace). Default: `"%m"`.
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

Copyright (C) 2016 Oliver Siegmar

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
