// src/main/java/Config.java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Config {
    private static final Config instance = new Config();
    private final Map<String, String> config = new ConcurrentHashMap<>();
    private Logger logger = Logger.getLogger(Config.class.getName());

    private boolean isReplica = false;

    private Config() {}

    public static Config getInstance() {
        return instance;
    }

    public void setConfig(String key, String value) {
        this.config.put(key, value);
    }

    public String getConfig(String key) {
        return this.config.get(key);
    }

    public void setReplica(boolean isReplica) {
        this.isReplica = isReplica;
    }

    public boolean isReplica() {
        return this.isReplica;
    }
}