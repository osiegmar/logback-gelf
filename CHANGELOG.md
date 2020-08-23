# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Add poolMaxIdleTime configuration option to TCP appenders
  [\#49](https://github.com/osiegmar/logback-gelf/pull/49)

### Changed
- Removed MD5 for creating Message-IDs and rewrote MessageIdSupplier logic (#52)
  [\#52](https://github.com/osiegmar/logback-gelf/issues/52)

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
- Simple connection pooling in GelfTcpAppender & round robin host lookup in GelfUdpAppender
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

[Unreleased]: https://github.com/osiegmar/logback-gelf/compare/v3.0.0...HEAD
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
