// src/main/java/Config.java
import java.util.HashMap;
import java.util.Map;

public class Config {
    private final Map<String, String> configMap = new HashMap<>();

    /**
     * Sets a configuration parameter.
     *
     * @param key the configuration parameter name
     * @param value the configuration parameter value
     */
    public void setConfig(String key, String value) {
        configMap.put(key, value);
    }

    /**
     * Retrieves the value of a configuration parameter.
     *
     * @param key the configuration parameter name
     * @return the configuration parameter value, or null if not found
     */
    public String getConfig(String key) {
        return configMap.get(key);
    }
}