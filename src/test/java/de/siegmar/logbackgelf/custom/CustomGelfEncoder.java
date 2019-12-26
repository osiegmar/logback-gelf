package de.siegmar.logbackgelf.custom;

import com.google.common.hash.Hashing;
import de.siegmar.logbackgelf.GelfEncoder;
import de.siegmar.logbackgelf.GelfMessage;

import java.nio.charset.StandardCharsets;

// Put it in different package from GelfEncoder to reveal any visibility issues
public class CustomGelfEncoder extends GelfEncoder {

  @Override
  protected String gelfMessageToJson(GelfMessage gelfMessage) {
    String json = super.gelfMessageToJson(gelfMessage);
    String sha256 = Hashing.sha256().hashString(json, StandardCharsets.UTF_8).toString();
    gelfMessage.getAdditionalFields().put("sha256", sha256);
    return super.gelfMessageToJson(gelfMessage);
  }

}
