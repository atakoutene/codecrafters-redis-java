import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;

// This is the protocol parser used to parse the commands
public class ProtocolParser {

    private static final Logger logger = Logger.getLogger(ProtocolParser.class.getName());

    // Get the singleton instance of the RedisServer
    private static final Cache redisServer = Cache.getInstance();

    // Get the persistent storage instance
    private static final Config CONFIG = Config.getInstance();

    public static String parse(String command, OutputStream out) throws IOException {
        logger.info("Received command: " + command);

        if (command == null || command.isEmpty()) {
            logger.warning("Command is null or empty");
            return null;
        }

        // Convert the command to uppercase
        String uppercasedCommand = command.toUpperCase(Locale.ROOT);
        logger.info("Uppercased command: " + uppercasedCommand);

        // Split the string according to the new line character \r\n
        String[] parts = command.split("\r\n");
        if (parts.length < 3) {
            logger.warning("Invalid command format");
            return "-ERR invalid command format\r\n";
        }

        // Remove the first three parts of the array as they are: number of arguments in given RESP array, command length and command
        parts = Arrays.copyOfRange(parts, 3, parts.length);
        logger.info("Command parts: " + Arrays.toString(parts));

        // Check if the command contains the INFO command
        if (uppercasedCommand.contains(CommandType.INFO.toString())) {
            return handleInfoCommand(parts);
        }
        // Check if the command contains the REPLCONF command
        else if (uppercasedCommand.contains(CommandType.REPLCONF.toString())) {
            return Master.handleReplconfCommand(parts, out);
        }
        // Check if the command contains the PING command
        else if (uppercasedCommand.contains(CommandType.PING.toString())) {
            return handlePingCommand();
        }
        // Check if the command contains the ECHO command
        else if (uppercasedCommand.contains(CommandType.ECHO.toString())) {
            return handleEchoCommand(parts);
        }
        else if (uppercasedCommand.contains(CommandType.CONFIG.toString())) {
            return handleConfigCommand(parts);
        }
        else if (uppercasedCommand.contains(CommandType.GET.toString())) {
            return handleGetCommand(parts);
        }
        else if (uppercasedCommand.contains(CommandType.SET.toString())) {
            return handleSetCommand(parts);
        }
        else if (uppercasedCommand.contains(CommandType.KEYS.toString())) {
            return handleKeyCommand(parts);
        }
        else if (uppercasedCommand.contains(CommandType.PSYNC.toString())) {
            return Master.handlePsyncCommand(parts, out);
        }
        // Check if the command contains an unknown command
        else {
            return handleUnknownCommand();
        }
    }

    public static String bulkString(String value) {
        return "$" + value.length() + "\r\n" + value + "\r\n";
    }

    private static String handleInfoCommand(String[] parts) {
        logger.info("Handling INFO command with parts: " + Arrays.toString(parts));

        if (parts.length > 1 && parts[1].equalsIgnoreCase("REPLICATION")) {
            return handleInfoReplicationCommand();
        }

        return "-ERR unknown section\r\n";
    }

    private static String handleInfoReplicationCommand() {
        logger.info("Handling INFO REPLICATION command");

        String role = CONFIG.getConfig("replicaof") != null ? "slave" : "master";
        String masterReplId = Master.getReplicationId();
        long masterReplOffset = Master.getReplicationOffset();

        String response = String.format("role:%s\r\nmaster_replid:%s\r\nmaster_repl_offset:%d\r\n", role, masterReplId, masterReplOffset);

        return bulkString(response);
    }
    private static String handlePingCommand() {
        logger.info("Handling PING command");
        return "+PONG\r\n";
    }

    private static String handleSetCommand(String[] parts) {
        logger.info("Handling SET command with parts: " + Arrays.toString(parts));

        // Get the key length and the key
        String key = parts[1];
        logger.info("Key: " + key);

        String value = parts[3];
        logger.info("Value: " + value);

        // Handle the expiry parameter
        if (parts.length > 7) {
            // Get the expiry parameter
            String parameter = parts[5];
            if (parameter.equalsIgnoreCase("px")) {
                logger.info("Expiry parameter detected: " + parameter);

                // Get the expiry time
                int expiryTime = Integer.parseInt(parts[7]);

                logger.info("Parsed expiry time: " + expiryTime + " milliseconds");

                // Get the current time
                long currentTime = System.currentTimeMillis();
                long expiryTimestamp = currentTime + expiryTime;

                redisServer.setWithExpiry(key, value, expiryTimestamp);

                logger.info("Set key with TTL: key=" + key + ", value=" + value + ", ttl=" + expiryTime + " milliseconds");

                return "+OK\r\n";
            }
        }

        // Set the key and value in the RedisServer
        redisServer.set(key, value);

        // Return the response to the user
        return "+OK\r\n";
    }

    private static String handleGetCommand(String[] parts) {
        logger.info("Handling GET command with parts: " + Arrays.toString(parts));

        // Get the key length and the key
        String key = parts[1];
        logger.info("Key: " + key);

        // Get the value associated to the key
        String value = redisServer.get(key);

        // Return the response to the user
        if (value == null) {
            logger.info("Value for key " + key + " not found");
            return "$-1\r\n";
        }

        logger.info("GET, this is the received value: " + value);
        return bulkString(value);
    }

    private static String handleEchoCommand(String[] parts) {
        logger.info("Handling ECHO command with parts: " + Arrays.toString(parts));

        // Return the numCharacters first characters of the last part
        String lastPart = parts[parts.length - 1];

        logger.info("Echo result: " + lastPart);

        return bulkString(lastPart);
    }

    private static String handleConfigCommand(String[] parts) {
        logger.info("Handling CONFIG command with parts: " + Arrays.toString(parts));

        // Get the command and parameter ([$3, GET, $3, dir])
        String command = parts[1];
        String parameter = parts[3];

        // If the command is GET
        if (command.equalsIgnoreCase("GET")) {
            String value = CONFIG.getConfig(parameter);

            // If there's no value associated with the parameter
            if (value == null) {
                return "*-1\r\n";
            }

            // Return the response to the user in RESP format
            return String.format("*2\r\n$%d\r\n%s\r\n$%d\r\n%s\r\n", parameter.length(), parameter, value.length(), value);
        }

        return "-ERR unknown command";
    }

    private static String handleKeyCommand(String[] parts) {
        logger.info("Handling KEYS command with * parts: " + Arrays.toString(parts));
        logger.info("Test");

        // Get the pattern to match
        String pattern = parts[1];

        logger.info("Getting keys");
        // Get all the keys that match the pattern, in this case we get all keys since the pattern is "*"
        String[] keys = redisServer.keys();

        logger.info("Got keys");
        logger.info("Keys: " + Arrays.toString(keys));

        // Filter the keys that match the pattern
        String filteredKeys = Arrays.stream(keys)
                .reduce("", (acc, key) -> acc + bulkString(key));

        logger.info("Filtered keys: " + filteredKeys);

        // Return the response to the user using this format "*1\r\n$3\r\nfoo\r\n"
        return String.format("*%d\r\n%s", keys.length, filteredKeys);
    }

    private static String handleUnknownCommand() {
        logger.warning("Handling unknown command");
        return "-ERR unknown command";
    }
}