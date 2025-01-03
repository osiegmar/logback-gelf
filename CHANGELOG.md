# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [6.1.1] - 2025-01-03
### Changed
- Update dependency to logback 1.5.15

## [6.1.0] - 2024-09-05
### Added
- Support for external encoders like Spring's StructuredLogEncoder

## [6.0.2] - 2024-08-24
### Changed
- Improve performance of number conversion in GelfEncoder (#108); Thanks to [@deathy](https://github.com/deathy)
- Update dependency to logback 1.5.7

## [6.0.1] - 2024-04-13
### Fixed
- Setting uri in GelfHttpAppender (#103)

## [6.0.0] - 2024-04-04
### Added
- HTTP appender for sending GELF messages via HTTP

### Changed
- Update dependency to logback 1.5.3
- Renamed shortPatternLayout to shortMessageLayout, fullPatternLayout to fullMessageLayout
  [\#100](https://github.com/osiegmar/logback-gelf/issues/100)

### Removed
- Removed support for truncating short messages (`maxShortMessageLength`)
  [\#100](https://github.com/osiegmar/logback-gelf/issues/100)

## [5.0.1] - 2023-12-10
### Changed
- Update to Logback 1.4.14

## [5.0.0] - 2023-10-21
### Added
- Support for key value pairs
  [\#86](https://github.com/osiegmar/logback-gelf/issues/86)
- Add another method for adding static field to GelfEncoder
  [\#80](https://github.com/osiegmar/logback-gelf/issues/80)
- Add support for truncating short messages (`maxShortMessageLength`)

### Changed
- Upgrade to Java 11 (Premier Support of Java 8 ended in March 2022).
- Upgrade to Logback 1.4.11
- GZIP Compression with GelfUdpAppender
  [\#66](https://github.com/osiegmar/logback-gelf/issues/66)
- Replace blank hostname and blank log message to prevent Graylog error
  [\#82](https://github.com/osiegmar/logback-gelf/issues/82)
- Default of `includeMarker` changed to `false`. Serialization format of markers has changed.
- Improved MessageID creation algorithm
- Improved JSON serialization performance

### Fixed
- Fixed build on windows

## [4.0.2] - 2021-12-22
### Changed
- Update dependency to logback 1.2.9
  [\#72](https://github.com/osiegmar/logback-gelf/issues/72)

## [4.0.1] - 2021-12-14
### Changed
- Update dependency to logback 1.2.8
  [\#67](https://github.com/osiegmar/logback-gelf/issues/67)

## [4.0.0] - 2021-10-17
### Added
- Add poolMaxIdleTime configuration option to TCP appenders (#49)
  [\#49](https://github.com/osiegmar/logback-gelf/pull/49)

### Changed
- Removed MD5 for creating Message-IDs and rewrote MessageIdSupplier logic (#52)
  [\#52](https://github.com/osiegmar/logback-gelf/issues/52)
- Ability to add custom fields to GelfMessage, computed from ILoggingEvent (#55)
  [\#55](https://github.com/osiegmar/logback-gelf/issues/55)
- Refactor `de.siegmar.logbackgelf.GelfMessage#toJSON` to return `byte[]`
  instead of String for proper performance. (#58)
  [\#58](https://github.com/osiegmar/logback-gelf/issues/58)
- Improve `SimpleJsonEncoder.escapeString` memory usage. (#61)
  [\#61](https://github.com/osiegmar/logback-gelf/issues/61)
- Defined 'de.siegmar.logbackgelf' as the Automatic-Module-Name (JPMS module name)

## [3.0.0] - 2020-03-15
### Added
- Allow encoder subclasses to customize the message before it is converted to String.
  [\#40](https://github.com/osiegmar/logback-gelf/issues/40)
- Server certificate hostname verification in `GelfTcpTlsAppender`.
- Allow custom implementations for supplying GELF UDP Message-IDs.

### Changed
- Upgrade to Java 8 (Premier Support of Java 7 ended in July 2019).
- Change the default value of `numbersAsString` of `GelfEncoder` from `true` to `false`.
- Rename `trustAllCertificates` property of `GelfTcpTlsAppender` to `insecure`.
- Never write timestamp in JSON using scientific notation.
- Never write static or MDC fields in JSON using scientific notation.

## [2.2.0] - 2019-12-14
### Added
- Add customizable keys for the logger name and thread name (#41)
  [\#41](https://github.com/osiegmar/logback-gelf/issues/41)

### Fixed
- Fix handling of multiple markers
  [\#35](https://github.com/osiegmar/logback-gelf/issues/35)

## [2.1.2] - 2019-11-04
### Fixed
- Build needs to be performed with Java < 9
  [\#38](https://github.com/osiegmar/logback-gelf/issues/38)

## [2.1.1] - 2019-11-03
### Fixed
- Fix empty hostname
  [\#34](https://github.com/osiegmar/logback-gelf/issues/34)

## [2.1.0] - 2019-06-12
### Changed
- Log numeric values as number (double precision) not string
  [\#30](https://github.com/osiegmar/logback-gelf/pull/30)

## [2.0.1] - 2019-05-19
### Fixed
- Reopen the UDP channel, if it was closed
  [\#20](https://github.com/osiegmar/logback-gelf/issues/20)

## [2.0.0] - 2019-02-12
### Changed
- Update dependency to logback 1.2.3
  [\#21](https://github.com/osiegmar/logback-gelf/issues/21)
- Changed implementation from Layout to Encoder (also renamed class GelfLayout to GelfEncoder and layout to encoder in GelfAppender)

## [1.1.0] - 2018-01-21
### Added
- Simple connection pooling in GelfTcpAppender & round-robin host lookup in GelfUdpAppender
  [\#11](https://github.com/osiegmar/logback-gelf/issues/11)

### Changed
- Update dependency to logback 1.1.8
  [\#1](https://github.com/osiegmar/logback-gelf/issues/1)

### Fixed
- The reconnect interval could not be disabled
  [\#12](https://github.com/osiegmar/logback-gelf/issues/12)

## [1.0.4] - 2017-04-03
### Added
- Support for GELF console logging (appendNewline in GelfLayout)

### Fixed
- Fix interrupted flag in GelfTcpAppender (restore flag after catching InterruptedException)

## [1.0.3] - 2017-01-12
### Added
- Support for logback 1.1.8
  [\#6](https://github.com/osiegmar/logback-gelf/issues/6)

## [1.0.2] - 2016-10-18
### Fixed
- Fix possible infinite loop bug with exception root cause

## [1.0.1] - 2016-10-08
### Added
- Support for forwarding of exception root cause
- Support for TLS encryption (with TCP)

## 1.0.0 - 2016-03-20

- Initial release

[Unreleased]: https://github.com/osiegmar/logback-gelf/compare/v6.1.1...HEAD
[6.1.1]: https://github.com/osiegmar/logback-gelf/compare/v6.1.0...v6.1.1
[6.1.0]: https://github.com/osiegmar/logback-gelf/compare/v6.0.2...v6.1.0
[6.0.2]: https://github.com/osiegmar/logback-gelf/compare/v6.0.1...v6.0.2
[6.0.1]: https://github.com/osiegmar/logback-gelf/compare/v6.0.0...v6.0.1
[6.0.0]: https://github.com/osiegmar/logback-gelf/compare/v5.0.1...v6.0.0
[5.0.1]: https://github.com/osiegmar/logback-gelf/compare/v5.0.0...v5.0.1
[5.0.0]: https://github.com/osiegmar/logback-gelf/compare/v4.0.2...v5.0.0
[4.0.2]: https://github.com/osiegmar/logback-gelf/compare/v4.0.1...v4.0.2
[4.0.1]: https://github.com/osiegmar/logback-gelf/compare/v4.0.0...v4.0.1
[4.0.0]: https://github.com/osiegmar/logback-gelf/compare/v3.0.0...v4.0.0
[3.0.0]: https://github.com/osiegmar/logback-gelf/compare/v2.2.0...v3.0.0
[2.2.0]: https://github.com/osiegmar/logback-gelf/compare/v2.1.2...v2.2.0
[2.1.2]: https://github.com/osiegmar/logback-gelf/compare/v2.1.1...v2.1.2
[2.1.1]: https://github.com/osiegmar/logback-gelf/compare/v2.1.0...v2.1.1
[2.1.0]: https://github.com/osiegmar/logback-gelf/compare/v2.0.1...v2.1.0
[2.0.1]: https://github.com/osiegmar/logback-gelf/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/osiegmar/logback-gelf/compare/v1.1.0...v2.0.0
[1.1.0]: https://github.com/osiegmar/logback-gelf/compare/v1.0.4...v1.1.0
[1.0.4]: https://github.com/osiegmar/logback-gelf/compare/v1.0.3...v1.0.4
[1.0.3]: https://github.com/osiegmar/logback-gelf/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/osiegmar/logback-gelf/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/osiegmar/logback-gelf/compare/v1.0.0...v1.0.1
