public class ProtocolParser {

    public static String parse(String command) {
        if (command.toUpperCase().contains("ECHO")) {
            String[] subCommands = command.split("\r\n");
            int n = subCommands.length;
            int length = Integer.parseInt(subCommands[n - 2].substring(1));
            String result = subCommands[n - 1].substring(0, length);
            return "$" + length + "\r\n" + result + "\r\n";

        } else if (command.toUpperCase().contains("PING")) {
            return "+PONG\r\n";
        } else if (command.toUpperCase().contains("GET")) {
            return "$5\r\nhello\r\n";
        } else if (command.toUpperCase().contains("SET")) {
            return "+OK\r\n";
        } else if (command.toUpperCase().contains("INCR")) {
            return ":1\r\n";
        } else if (command.toUpperCase().contains("DECR")) {
            return ":1\r\n";
        } else if (command.toUpperCase().contains("INCRBY")) {
            return ":2\r\n";
        } else if (command.toUpperCase().contains("DECRBY")) {
            return ":2\r\n";
        } else if (command.toUpperCase().contains("DEL")) {
            return ":1\r\n";
        } else if (command.toUpperCase().contains("EXISTS")) {
            return ":1\r\n";
        } else if (command.toUpperCase().contains("KEYS")) {
            return "*1\r\n$5\r\nhello\r\n";
        } else if (command.toUpperCase().contains("FLUSHALL")) {
            return "+OK\r\n";
        } else {
            return "-ERR unknown command '" + command + "'\r\n";
        }
    }


}
