package de.siegmar.logbackgelf;

import ch.qos.logback.classic.spi.ILoggingEvent;

public interface GelfAdditionalFieldMapper {

  KeyValue<String, Object> mapAdditionalField(ILoggingEvent event);

  class KeyValue<K, V> {
    final K key;
    final V value;

    public KeyValue(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public String toString() {
      return key + "=" + value;
    }
  }

}
