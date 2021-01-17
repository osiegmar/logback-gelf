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

import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.encoder.EncoderBase;
import de.siegmar.logbackgelf.mappers.CallerDataFieldMapper;
import de.siegmar.logbackgelf.mappers.MarkerFieldMapper;
import de.siegmar.logbackgelf.mappers.MdcDataFieldMapper;
import de.siegmar.logbackgelf.mappers.RootExceptionDataFieldMapper;
import de.siegmar.logbackgelf.mappers.SimpleFieldMapper;

/**
 * This class is responsible for transforming a Logback log event to a GELF message.
 */
@SuppressWarnings("checkstyle:classdataabstractioncoupling")
public class GelfEncoder extends EncoderBase<ILoggingEvent> {

    private static final Pattern VALID_ADDITIONAL_FIELD_PATTERN = Pattern.compile("^[\\w.-]*$");
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
     * Log numbers as String. Default: false.
     */
    private boolean numbersAsString;

    /**
     * Additional, static fields to send to graylog. Defaults: none.
     */
    private final Map<String, Object> staticFields = new HashMap<>();

    private final List<GelfFieldMapper<?>> builtInFieldMappers = new ArrayList<>();

    private final List<GelfFieldMapper<?>> fieldMappers = new ArrayList<>();

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
        return Collections.unmodifiableMap(staticFields);
    }

    public void addStaticField(final String staticField) {
        final String[] split = staticField.split(":", 2);
        if (split.length == 2) {
            try {
                addField(staticFields, split[0].trim(), split[1].trim());
            } catch (final IllegalArgumentException e) {
                addWarn("Could not add field " + staticField, e);
            }
        } else {
            addWarn("staticField must be in format key:value - rejecting '" + staticField + "'");
        }
    }

    public List<GelfFieldMapper<?>> getFieldMappers() {
        return Collections.unmodifiableList(fieldMappers);
    }

    public void addFieldMapper(final GelfFieldMapper<?> fieldMapper) {
        fieldMappers.add(fieldMapper);
    }

    private void addField(final Map<String, Object> dst, final String fieldName, final Object fieldValue) {
        if (fieldName.isEmpty()) {
            throw new IllegalArgumentException("fieldName key must not be empty");
        }
        if ("id".equalsIgnoreCase(fieldName)) {
            throw new IllegalArgumentException("fieldName key name 'id' is prohibited");
        }
        if (!VALID_ADDITIONAL_FIELD_PATTERN.matcher(fieldName).matches()) {
            throw new IllegalArgumentException("fieldName key '" + fieldName + "' is illegal. "
                + "Keys must apply to regex " + VALID_ADDITIONAL_FIELD_PATTERN);
        }

        final Object oldValue = dst.putIfAbsent(fieldName, convertToNumberIfNeeded(fieldValue));
        if (oldValue != null) {
            throw new IllegalArgumentException("Field mapper tried to set already defined key '" + fieldName + "'.");
        }
    }

    private Object convertToNumberIfNeeded(final Object value) {
        if (numbersAsString || !(value instanceof String)) {
            return value;
        }

        try {
            return new BigDecimal((String) value);
        } catch (final NumberFormatException e) {
            return value;
        }
    }

    @Override
    public void start() {
        if (originHost == null || originHost.trim().isEmpty()) {
            try {
                originHost = InetUtil.getLocalHostName();
            } catch (final UnknownHostException e) {
                addWarn("Could not determine local hostname", e);
                originHost = "unknown";
            }
        }
        if (shortPatternLayout == null) {
            shortPatternLayout = buildPattern(DEFAULT_SHORT_PATTERN);
        }
        if (fullPatternLayout == null) {
            fullPatternLayout = buildPattern(DEFAULT_FULL_PATTERN);
        }
        addBuiltInFieldMappers();

        super.start();
    }

    private PatternLayout buildPattern(final String pattern) {
        final PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(getContext());
        patternLayout.setPattern(pattern);
        patternLayout.start();
        return patternLayout;
    }

    private void addBuiltInFieldMappers() {
        builtInFieldMappers.add(new SimpleFieldMapper<>(loggerNameKey, ILoggingEvent::getLoggerName));
        builtInFieldMappers.add(new SimpleFieldMapper<>(threadNameKey, ILoggingEvent::getThreadName));

        if (includeLevelName) {
            builtInFieldMappers.add(new SimpleFieldMapper<>(levelNameKey, event -> event.getLevel().toString()));
        }

        if (includeRawMessage) {
            builtInFieldMappers.add(new SimpleFieldMapper<>("raw_message", ILoggingEvent::getMessage));
        }

        if (includeCallerData) {
            builtInFieldMappers.add(new CallerDataFieldMapper());
        }

        if (includeRootCauseData) {
            builtInFieldMappers.add(new RootExceptionDataFieldMapper());
        }

        if (includeMarker) {
            builtInFieldMappers.add(new MarkerFieldMapper("marker"));
        }

        if (includeMdcData) {
            builtInFieldMappers.add(new MdcDataFieldMapper());
        }
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] encode(final ILoggingEvent event) {
        final Map<String, Object> additionalFields = new HashMap<>(staticFields);
        addFieldMapperData(event, additionalFields, builtInFieldMappers);
        addFieldMapperData(event, additionalFields, fieldMappers);

        final GelfMessage gelfMessage = new GelfMessage(
            originHost,
            shortPatternLayout.doLayout(event),
            fullPatternLayout.doLayout(event),
            event.getTimeStamp(),
            LevelToSyslogSeverity.convert(event),
            additionalFields
        );

        String jsonStr = gelfMessageToJson(gelfMessage);
        if (appendNewline) {
            jsonStr += System.lineSeparator();
        }

        return jsonStr.getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void addFieldMapperData(final ILoggingEvent event, final Map<String, Object> additionalFields,
                                    final List<GelfFieldMapper<?>> mappers) {
        for (final GelfFieldMapper<?> fieldMapper : mappers) {
            try {
                fieldMapper.mapField(event, (key, value) -> {
                    try {
                        addField(additionalFields, key, value);
                    } catch (final IllegalArgumentException e) {
                        addWarn("Could not add field " + key, e);
                    }
                });
            } catch (final Exception e) {
                addError("Exception in field mapper", e);
            }
        }
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

}
