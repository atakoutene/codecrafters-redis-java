import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Cache {
    private int id;
    private final Map<String, String> keyToValue = new ConcurrentHashMap<>();
    private final Map<String, List<Object>> keyToValueWithExpiry = new ConcurrentHashMap<>();

    private static Cache instance;

    // Private constructor to enforce Singleton pattern
    private Cache() {}

    public static Cache getInstance() {
        if (instance == null) {
            synchronized (Cache.class) {
                if (instance == null) {
                    instance = new Cache();
                }
            }
        }
        return instance;
    }

    public synchronized void set(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value must not be null");
        }
        this.keyToValue.put(key, value);
    }

    public synchronized String get(String key) {
        if (keyToValue.containsKey(key)) {
            return keyToValue.get(key);
        } else if (keyToValueWithExpiry.containsKey(key)) {
            // Get the current time in milliseconds
            long currentTime = System.currentTimeMillis();
            List<Object> valueWithExpiry = keyToValueWithExpiry.get(key);

            // Check if the current time is less than the expiry time
            if (currentTime < (long) valueWithExpiry.get(1)) {
                return (String) valueWithExpiry.get(0);
            } else {
                keyToValueWithExpiry.remove(key);
                return null;
            }

        }
        return null;
    }

    public synchronized void setWithExpiry(String key, String value, long delay) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value must not be null");
        }

        List<Object> valueWithExpiry = List.of(value, delay);

        this.keyToValueWithExpiry.put(key, valueWithExpiry);
//        scheduler.schedule(() -> {
//            this.keyToValue.remove(key);
//            Logger logger = Logger.getLogger(Cache.class.getName());
//            logger.info("Removing key: " + key + " after " + delay + " " + TimeUnit.MILLISECONDS);
//        }, delay, TimeUnit.MILLISECONDS);
    }

    public synchronized void setWithExpiry(String key, String value, long delay, TimeUnit unit) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value must not be null");
        }

        List<Object> valueWithExpiry = unit == TimeUnit.SECONDS  ? List.of(value, delay * 1000) : List.of(value, delay);

        this.keyToValueWithExpiry.put(key, valueWithExpiry);
    }

    public synchronized String[] keys() {
        Set<String> allKeys = new HashSet<>(this.keyToValue.keySet());
        allKeys.addAll(this.keyToValueWithExpiry.keySet());
        return allKeys.toArray(new String[0]);
    }

    public synchronized void setId(int id) {
        this.id = id;
    }

    public synchronized int getId() {
        return this.id;
    }


}
