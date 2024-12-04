import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private Map<String, String> dataStore = new ConcurrentHashMap<>();
    private Map<String, Long> expiryStore = new ConcurrentHashMap<>();

    public synchronized void put(String key, String value, Long expiry) {
        dataStore.put(key, value);
        if (expiry != null) {
            expiryStore.put(key, expiry);
        }
    }

    public synchronized String get(String key) {
        if (dataStore.containsKey(key)) {
            String value = dataStore.get(key);
            Long expiry = expiryStore.get(key);
            if (expiry != null && expiry < System.currentTimeMillis()) {
                dataStore.remove(key);
                expiryStore.remove(key);
                return null;
            }
            return value;
        }
        return null;
    }

    public synchronized Map<String, String> getAll() {
        return dataStore;
    }

    public synchronized void remove(String key) {
        dataStore.remove(key);
        expiryStore.remove(key);
    }
}
