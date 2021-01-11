package de.siegmar.logbackgelf;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.function.BiConsumer;

/**
 * Field mapper that can be used to add fields to resulting GELF message, using {@link ILoggingEvent} as input.
 */
public interface GelfFieldMapper {

  /**
   * Map a field from {@link ILoggingEvent} to a GELF message.
   *
   * @param event the source log event
   * @param valueHandler the consumer of the field ({@link String} name and {@link Object} value)
   */
  void mapField(ILoggingEvent event, BiConsumer<String, Object> valueHandler);

}
