import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Cache {
    private static final ConcurrentHashMap<String, String> dataStore = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Logger logger = Logger.getLogger(Cache.class.getName());

    /**
     * Retrieves the value associated with the given key.
     *
     * @param key the key to retrieve the value for
     * @return the value associated with the key, or null if the key does not exist
     */
    public String get(String key) {
        logger.info("Getting value for key: " + key);
        return dataStore.getOrDefault(key, null);
    }

    /**
     * Sets the value for the given key.
     *
     * @param key the key to set the value for
     * @param value the value to set
     */
    public void set(String key, String value) {
        logger.info("Setting value for key: " + key);
        dataStore.put(key, value);
    }

    /**
     * Sets the value for the given key with a time-to-live (TTL) in milliseconds.
     *
     * @param key the key to set the value for
     * @param value the value to set
     * @param ttl the time-to-live in milliseconds
     */
    public void setWithExpiry(String key, String value, long ttl) {
        logger.info("Setting value for key: " + key + " with TTL: " + ttl + " milliseconds");
        dataStore.put(key, value);
        Runnable task = () -> {
            dataStore.remove(key);
            logger.info("Key expired and removed: " + key);
        };
        scheduler.schedule(task, ttl, TimeUnit.MILLISECONDS);
    }
}