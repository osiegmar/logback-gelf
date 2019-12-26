package de.siegmar.logbackgelf;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.siegmar.logbackgelf.custom.CustomGelfEncoder;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static de.siegmar.logbackgelf.GelfEncoderTest.basicValidation;
import static de.siegmar.logbackgelf.GelfEncoderTest.simpleLoggingEvent;
import static org.junit.Assert.assertEquals;

public class CustomGelfEncoderTest {

  private static final String LOGGER_NAME = GelfEncoderTest.class.getCanonicalName();
  private static final long TIMESTAMP = 1577359700000L;

  private final CustomGelfEncoder encoder = new CustomGelfEncoder();

  @Before
  public void before() {
    encoder.setContext(new LoggerContext());
    encoder.setOriginHost("localhost");
  }

  @Test
  public void custom() throws IOException {
    encoder.start();

    final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    final Logger logger = lc.getLogger(LOGGER_NAME);

    final LoggingEvent event = simpleLoggingEvent(logger, null);
    event.setTimeStamp(TIMESTAMP);
    final String logMsg = encodeToStr(event);

    final ObjectMapper om = new ObjectMapper();
    final JsonNode jsonNode = om.readTree(logMsg);
    basicValidation(jsonNode);

    assertEquals("message 1\n", jsonNode.get("full_message").textValue());
    assertEquals(
        "42d545b39843028e578fd8c2b2470b905586c028c6eb0ed218ec1dc6ea869ed3",
        jsonNode.get("_sha256").textValue()
    );
  }

  private String encodeToStr(final LoggingEvent event) {
    return new String(encoder.encode(event), StandardCharsets.UTF_8);
  }

}
