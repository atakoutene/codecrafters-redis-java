import java.util.Arrays;

public class ProtocolParser {

    private static final String ECHO = "ECHO";
    private static final String PING = "PING";
    private static final String GET = "GET";
    private static final String SET = "SET";

    private final RedisServer redisServer;

    public ProtocolParser(RedisServer redisServer) {
        this.redisServer = redisServer;
    }

    public String parse(String command) {
        String[] tmp = command.split("\r\n");
        String[] subCommands = Arrays.copyOfRange(tmp, 1, tmp.length);
        String upperCommand = command.toUpperCase();

        if (upperCommand.contains(ECHO)) {
            return handleEcho(tmp);
        } else if (upperCommand.contains(PING)) {
            return handlePing();
        } else if (upperCommand.contains(GET)) {
            return handleGet(subCommands);
        } else if (upperCommand.contains(SET)) {
            return handleSet(subCommands);
        } else {
            return handleUnknownCommand(command);
        }
    }

    private String handleEcho(String[] subCommands) {
        int n = subCommands.length;
        int length = Integer.parseInt(subCommands[n - 2].substring(1));
        String result = subCommands[n - 1].substring(0, length);
        return "$" + length + "\r\n" + result + "\r\n";
    }

    private String handlePing() {
        return "+PONG\r\n";
    }

    private String handleGet(String[] subCommands) {
        String key = subCommands[3];
        String value = redisServer.get(key);
        return value == null ? "$-1\r\n" : String.format("$%d\r\n%s\r\n", value.length(), value);
    }

    private String handleSet(String[] subCommands) {
        String key = subCommands[3];
        String value = subCommands[5];
        redisServer.set(key, value);
        return "+OK\r\n";
    }

    private String handleUnknownCommand(String command) {
        return "-ERR unknown command '" + command + "'\r\n";
    }
}