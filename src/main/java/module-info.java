module de.siegmar.logbackgelf {

    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires java.naming;
    requires java.net.http;
    requires org.slf4j;

    exports de.siegmar.logbackgelf;
    exports de.siegmar.logbackgelf.compressor;
    exports de.siegmar.logbackgelf.mappers;
    exports de.siegmar.logbackgelf.pool;

}
