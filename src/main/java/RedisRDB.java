import java.util.HashMap;
import java.util.Map;

public class RedisRDB {
    String header;
    Map<String, String> keyValues;

    public RedisRDB() {
        this.keyValues = new HashMap<>();
    }
}