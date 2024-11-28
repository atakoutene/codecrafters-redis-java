// src/main/java/ProtocolParser.java
import java.util.logging.Logger;

public class ProtocolParser {

    private static final String ECHO = "ECHO";
    private static final String PING = "PING";
    private static final String GET = "GET";
    private static final String CONFIG_GET = "CONFIG";
    private static final String SET = "SET";
    private static final Logger logger = Logger.getLogger(ProtocolParser.class.getName());

    private final Cache cache;
    private final Config config;

    public ProtocolParser(Cache cache, Config config) {
        this.cache = cache;
        this.config = config;
    }

    /**
     * Parses the given command and delegates to the appropriate handler method.
     *
     * @param command the command to parse
     * @return the response from the command execution
     */
    public String parse(String command) {
        logger.info("Parsing command: " + command);
        // Split the command into subcommands according to the end of line characters
        String[] subCommands = command.split("\r\n");
        // Transform the command to uppercase
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
        } else {
            return handleUnknownCommand(command);
        }
    }

    /**
     * Handles the CONFIG GET command.
     *
     * @param subCommands the sub-commands of the CONFIG GET command
     * @return the response from the CONFIG GET command
     */
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

    /**
     * Handles the ECHO command.
     *
     * @param subCommands the sub-commands of the ECHO command
     * @return the response from the ECHO command
     */
    private String handleEcho(String[] subCommands) {
        logger.info("Handling ECHO command");
        int n = subCommands.length;
        // Get the length of the string to be echoed, it's the second last element
        int length = Integer.parseInt(subCommands[n - 2].substring(1));
        // Get the string to be echoed, it's the last element
        String result = subCommands[n - 1];
        return "$" + length + "\r\n" + result + "\r\n";
    }

    /**
     * Handles the PING command.
     *
     * @return the response from the PING command
     */
    private String handlePing() {
        logger.info("Handling PING command");
        return "+PONG\r\n";
    }

    /**
     * Handles the GET command.
     *
     * @param subCommands the sub-commands of the GET command
     * @return the response from the GET command
     */
    private String handleGet(String[] subCommands) {
        logger.info("Handling GET command");
        // Get the key from the command, it's the 4th index
        String key = subCommands[4];
        // Get the value from the Redis server
        String value = cache.get(key);
        return value == null ? "$-1\r\n" : String.format("$%d\r\n%s\r\n", value.length(), value);
    }

    /**
     * Handles the SET command.
     *
     * @param subCommands the sub-commands of the SET command
     * @return the response from the SET command
     */
    private String handleSet(String[] subCommands) {
        logger.info("Handling SET command");
        // Get the key and value from the command, they are the 4th and 6th index
        String key = subCommands[4];
        String value = subCommands[6];
        // Check if the command has an expiry time based on the length of the subCommands array
        // PX is the 8th index
        if (subCommands.length > 7 && subCommands[8].equalsIgnoreCase("PX")) {
            logger.info("Setting with expiry");
            logger.info("Setting with expiry " + subCommands[10]);
            // Get the time-to-live (TTL) from the command, it's the 10th index
            long ttl = Long.parseLong(subCommands[10]);
            // Set the value with the expiry time
            cache.setWithExpiry(key, value, ttl);
        } else {
            cache.set(key, value);
        }
        return "+OK\r\n";
    }

    /**
     * Handles unknown commands.
     *
     * @param command the unknown command
     * @return the error response for the unknown command
     */
    private String handleUnknownCommand(String command) {
        logger.warning("Unknown command: " + command);
        return "-ERR unknown command '" + command + "'\r\n";
    }
}