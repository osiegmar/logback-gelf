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
import java.util.Map;
import java.util.regex.Pattern;

import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * A helper utility for field validation and conversion.
 */
public class GelfFieldHelper {

    private static final Pattern VALID_ADDITIONAL_FIELD_PATTERN = Pattern.compile("^[\\w.-]*$");

    private final ContextAwareBase logger;

    /**
     * Log numbers as String. Default: false.
     */
    private boolean numbersAsString;

    GelfFieldHelper(final ContextAwareBase logger) {
        this.logger = logger;
    }

    /**
     * @param fieldName name of the field to check
     * @return true in case field name is valid; false otherwise
     */
    public boolean isValidFieldName(final String fieldName) {
        boolean isValid = true;
        if (fieldName.isEmpty()) {
            logger.addWarn("staticField key must not be empty");
            isValid = false;
        } else if ("id".equalsIgnoreCase(fieldName)) {
            logger.addWarn("staticField key name 'id' is prohibited");
            isValid = false;
        } else if (!VALID_ADDITIONAL_FIELD_PATTERN.matcher(fieldName).matches()) {
            logger.addWarn("staticField key '" + fieldName + "' is illegal. "
                    + "Keys must apply to regex ^[\\w.-]*$");
            isValid = false;
        }
        return isValid;
    }

    /**
     * Check validity of field name and in case it is valid, add it to the provided map
     * (converting the value to a number, if needed).
     *
     * @param dst the map where to add the field
     * @param fieldName field name
     * @param fieldValue field value
     */
    public void addField(final Map<String, Object> dst, final String fieldName, final String fieldValue) {
        if (isValidFieldName(fieldName) && fieldValue != null) {
            dst.put(fieldName, convertToNumberIfNeeded(fieldValue));
        }
    }

    /**
     * In case {@link #isValidFieldName(String)} is enabled, try to convert the provided value to a number.
     *
     * @param value the value to convert
     * @return the provided value as {@link String}, in case {@link #isValidFieldName(String)} is disabled or
     *         conversion fails; otherwise return the provided value as {@link BigDecimal}.
     */
    public Object convertToNumberIfNeeded(final String value) {
        if (!numbersAsString) {
            try {
                return new BigDecimal(value);
            } catch (final NumberFormatException e) {
                return value;
            }
        }
        return value;
    }

    public void setNumbersAsString(final boolean numbersAsString) {
        this.numbersAsString = numbersAsString;
    }

    public boolean isNumbersAsString() {
        return numbersAsString;
    }

}
