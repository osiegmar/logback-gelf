# Upgrading

Some changes may require to update your configuration.

## Upgrade from 2.x to 3.x

* Version 3.0.0 of this library upgraded from Java 7 to Java 8.
* The server's certificate hostname now gets verified by `GelfTcpTlsAppender`.
* The `trustAllCertificates` property of `GelfTcpTlsAppender` was renamed to `insecure`.
* The default value of `numbersAsString` of `GelfEncoder` was changed from `true` to `false`.

## Upgrade from 1.x to 2.x

* Version 2.0.0 of this library introduced a configuration change.

**Old** format:
```xml
<layout class="de.siegmar.logbackgelf.GelfLayout">
    ...
</layout>
```

**New** format:
```xml
<encoder class="de.siegmar.logbackgelf.GelfEncoder">
    ...
</encoder>
```

This change was introduced, as the API of the Encoder interface changed in Logback 1.2.0.
