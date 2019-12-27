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

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.slf4j.Marker;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.encoder.EncoderBase;


/**
 * This class is responsible for transforming a Logback log event to a GELF message.
 */
public class GelfEncoder extends EncoderBase<ILoggingEvent> {

    private static final Pattern VALID_ADDITIONAL_FIELD_PATTERN = Pattern.compile("^[\\w.-]*$");
    private static final double MSEC_DIVIDER = 1000D;

    private static final String DEFAULT_SHORT_PATTERN = "%m%nopex";
    private static final String DEFAULT_FULL_PATTERN = "%m%n";

    /**
     * Origin hostname - will be auto detected if not specified.
     */
    private String originHost;

    /**
     * If true, the raw message (with argument placeholders) will be sent, too. Default: false.
     */
    private boolean includeRawMessage;

    /**
     * If true, logback markers will be sent, too. Default: true.
     */
    private boolean includeMarker = true;

    /**
     * If true, MDC keys/values will be sent, too. Default: true.
     */
    private boolean includeMdcData = true;

    /**
     * If true, caller data (source file-, method-, class name and line) will be sent, too.
     * Default: false.
     */
    private boolean includeCallerData;

    /**
     * If true, root cause exception of the exception passed with the log message will be
     * exposed in the exception field. Default: false.
     */
    private boolean includeRootCauseData;

    /**
     * If true, the log level name (e.g. DEBUG) will be sent, too. Default: false.
     */
    private boolean includeLevelName;

    /**
     * The key that should be used for the levelName.
     */
    private String levelNameKey = "level_name";

    /**
     * The key that should be used for the loggerName.
     */
    private String loggerNameKey = "logger_name";

    /**
     * The key that should be used for the threadName.
     */
    private String threadNameKey = "thread_name";

    /**
     * If true, a system depended newline separator will be added at the end of each message.
     * Don't use this in conjunction with TCP or UDP appenders, as this is only reasonable for
     * console logging!
     * Default: false.
     */
    private boolean appendNewline;

    /**
     * Short message format. Default: `"%m%nopex"`.
     */
    private PatternLayout shortPatternLayout;

    /**
     * Full message format (Stacktrace). Default: `"%m%n"`.
     */
    private PatternLayout fullPatternLayout;

    /**
     * Log numbers as String. Default: true (will be changed in next major release).
     */
    private boolean numbersAsString = true;

    /**
     * Additional, static fields to send to graylog. Defaults: none.
     */
    private Map<String, Object> staticFields = new HashMap<>();

    public String getOriginHost() {
        return originHost;
    }

    public void setOriginHost(final String originHost) {
        this.originHost = originHost;
    }

    public boolean isIncludeRawMessage() {
        return includeRawMessage;
    }

    public void setIncludeRawMessage(final boolean includeRawMessage) {
        this.includeRawMessage = includeRawMessage;
    }

    public boolean isIncludeMarker() {
        return includeMarker;
    }

    public void setIncludeMarker(final boolean includeMarker) {
        this.includeMarker = includeMarker;
    }

    public boolean isIncludeMdcData() {
        return includeMdcData;
    }

    public void setIncludeMdcData(final boolean includeMdcData) {
        this.includeMdcData = includeMdcData;
    }

    public boolean isIncludeCallerData() {
        return includeCallerData;
    }

    public void setIncludeCallerData(final boolean includeCallerData) {
        this.includeCallerData = includeCallerData;
    }

    public boolean isIncludeRootCauseData() {
        return includeRootCauseData;
    }

    public void setIncludeRootCauseData(final boolean includeRootCauseData) {
        this.includeRootCauseData = includeRootCauseData;
    }

    public boolean isIncludeLevelName() {
        return includeLevelName;
    }

    public void setIncludeLevelName(final boolean includeLevelName) {
        this.includeLevelName = includeLevelName;
    }

    public String getLevelNameKey() {
        return levelNameKey;
    }

    public void setLevelNameKey(final String levelNameKey) {
        this.levelNameKey = levelNameKey;
    }

    public String getLoggerNameKey() {
        return loggerNameKey;
    }

    public void setLoggerNameKey(final String loggerNameKey) {
        this.loggerNameKey = loggerNameKey;
    }

    public String getThreadNameKey() {
        return threadNameKey;
    }

    public void setThreadNameKey(final String threadNameKey) {
        this.threadNameKey = threadNameKey;
    }

    public boolean isAppendNewline() {
        return appendNewline;
    }

    public void setAppendNewline(final boolean appendNewline) {
        this.appendNewline = appendNewline;
    }

    public boolean isNumbersAsString() {
        return numbersAsString;
    }

    public void setNumbersAsString(final boolean numbersAsString) {
        this.numbersAsString = numbersAsString;
    }

    public PatternLayout getShortPatternLayout() {
        return shortPatternLayout;
    }

    public void setShortPatternLayout(final PatternLayout shortPatternLayout) {
        this.shortPatternLayout = shortPatternLayout;
    }

    public PatternLayout getFullPatternLayout() {
        return fullPatternLayout;
    }

    public void setFullPatternLayout(final PatternLayout fullPatternLayout) {
        this.fullPatternLayout = fullPatternLayout;
    }

    public Map<String, Object> getStaticFields() {
        return staticFields;
    }

    public void setStaticFields(final Map<String, Object> staticFields) {
        this.staticFields = Objects.requireNonNull(staticFields);
    }

    public void addStaticField(final String staticField) {
        final String[] split = staticField.split(":", 2);
        if (split.length == 2) {
            addField(staticFields, split[0].trim(), split[1].trim());
        } else {
            addWarn("staticField must be in format key:value - rejecting '" + staticField + "'");
        }
    }

    private void addField(final Map<String, Object> dst, final String key, final String value) {
        if (key.isEmpty()) {
            addWarn("staticField key must not be empty");
        } else if ("id".equalsIgnoreCase(key)) {
            addWarn("staticField key name 'id' is prohibited");
        } else if (dst.containsKey(key)) {
            addWarn("additional field with key '" + key + "' is already set");
        } else if (!VALID_ADDITIONAL_FIELD_PATTERN.matcher(key).matches()) {
            addWarn("staticField key '" + key + "' is illegal. "
                + "Keys must apply to regex ^[\\w.-]*$");
        } else {
            if (value != null) {
                dst.put(key, processValue(value));
            }
        }
    }

    private Object processValue(final String value) {
        if (!numbersAsString) {
            try {
                return Double.valueOf(value);
            } catch (final NumberFormatException e) {
                return value;
            }
        }
        return value;
    }

    @Override
    public void start() {
        if (originHost == null || originHost.trim().isEmpty()) {
            originHost = buildHostname();
        }
        if (shortPatternLayout == null) {
            shortPatternLayout = buildPattern(DEFAULT_SHORT_PATTERN);
        }
        if (fullPatternLayout == null) {
            fullPatternLayout = buildPattern(DEFAULT_FULL_PATTERN);
        }

        super.start();
    }

    private String buildHostname() {
        try {
            return InetUtil.getLocalHostName();
        } catch (final UnknownHostException e) {
            addWarn("Could not determine local hostname", e);
            return "unknown";
        }
    }

    private PatternLayout buildPattern(final String pattern) {
        final PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(getContext());
        patternLayout.setPattern(pattern);
        patternLayout.start();
        return patternLayout;
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] encode(final ILoggingEvent event) {
        final String shortMessage = shortPatternLayout.doLayout(event);
        final String fullMessage = fullPatternLayout.doLayout(event);
        final double timestamp = event.getTimeStamp() / MSEC_DIVIDER;
        final Map<String, Object> additionalFields = mapAdditionalFields(event);

        final GelfMessage gelfMessage =
            new GelfMessage(originHost, shortMessage, fullMessage, timestamp,
                LevelToSyslogSeverity.convert(event), additionalFields);

        String jsonStr = gelfMessageToJson(gelfMessage);
        if (appendNewline) {
            jsonStr += System.lineSeparator();
        }

        return jsonStr.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Allow subclasses to customize the message before it is converted to String.
     *
     * @param gelfMessage the GELF message to serialize.
     * @return the serialized GELF message (in JSON format).
     */
    protected String gelfMessageToJson(final GelfMessage gelfMessage) {
        return gelfMessage.toJSON();
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    private Map<String, Object> mapAdditionalFields(final ILoggingEvent event) {
        final Map<String, Object> additionalFields = new HashMap<>(staticFields);

        additionalFields.put(loggerNameKey, event.getLoggerName());
        additionalFields.put(threadNameKey, event.getThreadName());

        if (includeRawMessage) {
            additionalFields.put("raw_message", event.getMessage());
        }

        if (includeMarker) {
            final Marker marker = event.getMarker();
            if (marker != null) {
                additionalFields.put("marker", buildMarkerStr(marker));
            }
        }

        if (includeLevelName) {
            additionalFields.put(levelNameKey, event.getLevel().levelStr);
        }

        if (includeMdcData) {
            additionalFields.putAll(buildMdcData(event.getMDCPropertyMap()));
        }

        if (includeCallerData) {
            additionalFields.putAll(buildCallerData(event.getCallerData()));
        }

        if (includeRootCauseData) {
            additionalFields.putAll(buildRootExceptionData(event.getThrowableProxy()));
        }

        return additionalFields;
    }

    private static String buildMarkerStr(final Marker marker) {
        if (!marker.hasReferences()) {
            return marker.getName();
        }

        final StringBuilder sb = new StringBuilder(marker.getName());

        final Iterator<Marker> it = marker.iterator();
        do {
            sb.append(", ").append(it.next().getName());
        } while (it.hasNext());

        return sb.toString();
    }

    private Map<String, Object> buildMdcData(final Map<String, String> mdcProperties) {
        if (mdcProperties == null || mdcProperties.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, Object> additionalFields = new HashMap<>();
        for (final Map.Entry<String, String> entry : mdcProperties.entrySet()) {
            addField(additionalFields, entry.getKey(), entry.getValue());
        }

        return additionalFields;
    }

    private Map<String, Object> buildCallerData(final StackTraceElement[] callerData) {
        if (callerData == null || callerData.length == 0) {
            return Collections.emptyMap();
        }

        final StackTraceElement first = callerData[0];

        final Map<String, Object> callerDataMap = new HashMap<>(4);
        callerDataMap.put("source_file_name", first.getFileName());
        callerDataMap.put("source_method_name", first.getMethodName());
        callerDataMap.put("source_class_name", first.getClassName());
        callerDataMap.put("source_line_number", first.getLineNumber());

        return callerDataMap;
    }

    private Map<String, Object> buildRootExceptionData(final IThrowableProxy throwableProxy) {
        final IThrowableProxy rootException = getRootException(throwableProxy);
        if (rootException == null) {
            return Collections.emptyMap();
        }

        final Map<String, Object> exceptionDataMap = new HashMap<>(2);
        exceptionDataMap.put("root_cause_class_name", rootException.getClassName());
        exceptionDataMap.put("root_cause_message", rootException.getMessage());

        return exceptionDataMap;
    }

    private IThrowableProxy getRootException(final IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return null;
        }

        IThrowableProxy rootCause = throwableProxy;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        return rootCause;
    }

}
