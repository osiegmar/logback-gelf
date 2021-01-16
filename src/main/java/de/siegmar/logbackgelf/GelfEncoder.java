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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     * Additional, static fields to send to graylog. Defaults: none.
     */
    private Map<String, Object> staticFields = new HashMap<>();

    private final List<GelfFieldMapper<?>> fieldMappers = new ArrayList<>();

    private final GelfFieldHelper fieldHelper = new GelfFieldHelper(this);

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
        return fieldHelper.isNumbersAsString();
    }

    public void setNumbersAsString(final boolean numbersAsString) {
        this.fieldHelper.setNumbersAsString(numbersAsString);
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
            fieldHelper.addField(staticFields, split[0].trim(), split[1].trim());
        } else {
            addWarn("staticField must be in format key:value - rejecting '" + staticField + "'");
        }
    }

    public void addFieldMapper(final GelfFieldMapper<?> fieldMapper) {
        fieldMappers.add(fieldMapper);
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

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] encode(final ILoggingEvent event) {
        final String shortMessage = shortPatternLayout.doLayout(event);
        final String fullMessage = fullPatternLayout.doLayout(event);
        final Map<String, Object> additionalFields = new HashMap<>(staticFields);

        fieldMappers.forEach(fieldMapper -> fieldMapper.mapField(event, (key, value) -> {
            final Object oldValue = additionalFields.put(key, value);
            if (oldValue != null) {
                addWarn("additional field with key '" + key + "' is already set");
                additionalFields.put(key, oldValue);
            }
        }));

        final GelfMessage gelfMessage = new GelfMessage(originHost, shortMessage, fullMessage,
            event.getTimeStamp(), LevelToSyslogSeverity.convert(event), additionalFields);

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

    private void addBuiltInFieldMappers() {
        addFieldMapper(new SimpleFieldMapper<>(loggerNameKey, ILoggingEvent::getLoggerName));
        addFieldMapper(new SimpleFieldMapper<>(threadNameKey, ILoggingEvent::getThreadName));

        if (includeRawMessage) {
            addFieldMapper(new SimpleFieldMapper<>("raw_message", ILoggingEvent::getMessage));
        }

        if (includeMarker) {
            addFieldMapper(new MarkerFieldMapper("marker"));
        }

        if (includeLevelName) {
            addFieldMapper(new SimpleFieldMapper<>(levelNameKey, event -> event.getLevel().levelStr));
        }

        if (includeMdcData) {
            addFieldMapper(new MdcDataFieldMapper(fieldHelper));
        }

        if (includeCallerData) {
            addFieldMapper(new CallerDataFieldMapper());
        }

        if (includeRootCauseData) {
            addFieldMapper(new RootExceptionDataFieldMapper());
        }
    }

}
