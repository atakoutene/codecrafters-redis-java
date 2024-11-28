import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ProtocolParser {

    private static final String ECHO = "ECHO";
    private static final String PING = "PING";
    private static final String GET = "GET";
    private static final String CONFIG_GET = "CONFIG";
    private static final String SET = "SET";
    private static final String KEYS = "KEYS";
    private static final Logger logger = Logger.getLogger(ProtocolParser.class.getName());

    private final Cache cache;
    private final Config config;

    public ProtocolParser(Cache cache, Config config) {
        this.cache = cache;
        this.config = config;
    }

    public String parse(String command) {
        logger.info("Parsing command: " + command);
        String[] subCommands = command.split("\r\n");
        String upperCommand = command.toUpperCase();

        if (upperCommand.contains(ECHO)) {
            return handleEcho(subCommands);
        } else if (upperCommand.contains(PING)) {
            return handlePing();
        } else if (upperCommand.contains(CONFIG_GET)) {
            return handleConfigGet(subCommands);
        } else if (upperCommand.contains(GET)) {
            return handleGet(subCommands);
        } else if (upperCommand.contains(SET)) {
            return handleSet(subCommands);
        } else if (upperCommand.contains(KEYS)) {
            return handleKeys(subCommands);
        } else {
            return handleUnknownCommand(command);
        }
    }

    private String handleKeys(String[] subCommands) {
        logger.info("Handling KEYS command");
//        String pattern = subCommands[4];
        String pattern = "*";
        String[] keys = cache.getKeys(pattern);
        StringBuilder result = new StringBuilder();
        result.append("*").append(keys.length).append("\r\n");
        for (String key : keys) {
            result.append("$").append(key.length()).append("\r\n");
            result.append(key).append("\r\n");
        }
        return result.toString();
    }

    private String handleConfigGet(String[] subCommands) {
        logger.info("Handling CONFIG GET command");
        String parameter = subCommands[6];
        String value = config.getConfig(parameter);

        if (value != null) {
            return String.format("*2\r\n$%d\r\n%s\r\n$%d\r\n%s\r\n", parameter.length(), parameter, value.length(), value);
        } else {
            return "-ERR unknown parameter\r\n";
        }
    }

    private String handleEcho(String[] subCommands) {
        logger.info("Handling ECHO command");
        int n = subCommands.length;
        int length = Integer.parseInt(subCommands[n - 2].substring(1));
        String result = subCommands[n - 1];
        return "$" + length + "\r\n" + result + "\r\n";
    }

    private String handlePing() {
        logger.info("Handling PING command");
        return "+PONG\r\n";
    }

    private String handleGet(String[] subCommands) {
        logger.info("Handling GET command");
        String key = subCommands[4];
        String value = cache.get(key);
        return value == null ? "$-1\r\n" : String.format("$%d\r\n%s\r\n", value.length(), value);
    }

    private String handleSet(String[] subCommands) {
        logger.info("Handling SET command");
        String key = subCommands[4];
        String value = subCommands[6];
        if (subCommands.length > 7 && subCommands[8].equalsIgnoreCase("PX")) {
            long ttl = Long.parseLong(subCommands[10]);
            cache.setWithExpiry(key, value, ttl, TimeUnit.MILLISECONDS);
        } else {
            cache.set(key, value);
        }
        return "+OK\r\n";
    }

    private String handleUnknownCommand(String command) {
        logger.warning("Unknown command: " + command);
        return "-ERR unknown command '" + command + "'\r\n";
    }
}