import java.util.concurrent.ConcurrentHashMap;

public class RedisServer {
    private static final ConcurrentHashMap<String, String> dataStore = new ConcurrentHashMap<>();

    public String get(String key) {
        return dataStore.getOrDefault(key, null);
    }

    public void set(String key, String value) {
        dataStore.put(key, value);
    }


}
