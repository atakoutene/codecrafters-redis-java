import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Config {
    // Singleton instance
    private static final Config instance = new Config();
    // Store to hold the key-value pairs
    private final Map<String, String> config = new ConcurrentHashMap<>();
    private Logger logger = Logger.getLogger(Config.class.getName());

    static Map<Integer, Object> table = new ConcurrentHashMap<>();

    private Config() {
    }

    public static Config getInstance() {
        return instance;
    }

    public void setConfig(String key, String value) {
        this.config.put(key, value);
    }


    public String getConfig(String key) {
        return this.config.get(key);
    }


}
