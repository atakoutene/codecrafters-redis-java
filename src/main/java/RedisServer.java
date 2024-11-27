import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RedisServer {
    private static final ConcurrentHashMap<String, String> dataStore = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Logger logger = Logger.getLogger(RedisServer.class.getName());

    public String get(String key) {
        logger.info("Getting value for key: " + key);
        return dataStore.getOrDefault(key, null);
    }

    public void set(String key, String value) {
        logger.info("Setting value for key: " + key);
        dataStore.put(key, value);
    }

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