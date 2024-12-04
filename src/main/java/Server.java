import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class Server {
    private static final int PORT = 6379;
    private DataStore dataStore;
    private static ProtocolParser protocolParser;

    public Server(DataStore dataStore, ProtocolParser protocolParser) {
        this.dataStore = dataStore;
        this.protocolParser = protocolParser;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream())) {

            String[] lines;
            while ((lines = readLines(in)) != null) {
                System.out.println("Received: " + Arrays.toString(lines));
                handleCommand(lines, out);
            }

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    private void handleCommand(String[] lines, PrintWriter out) {
        switch (lines[2].toLowerCase()) {
            case "ping":
                out.print("+PONG\r\n");
                break;
            case "echo":
                response(out, lines[4]);
                break;
            case "set":
                String key = lines[4];
                String value = lines[6];
                Long expiry = (lines.length == 11) ? Long.parseLong(lines[10]) + System.currentTimeMillis() : null;
                dataStore.put(key, value, expiry);
                out.print("+OK\r\n");
                break;
            case "get":
                handleGetCommand(lines, out);
                break;
            case "config":
                handleConfigCommand(lines, out);
                break;
            case "keys":
                handleKeysCommand(out);
                break;
            default:
                out.print("-ERR unknown command\r\n");
                break;
        }
        out.flush();
    }

    private void handleGetCommand(String[] lines, PrintWriter out) {
        String key = lines[4];
        String value = dataStore.get(key);
        if (value == null) {
            out.print("$-1\r\n");
        } else {
            response(out, value);
        }
    }

    private void handleConfigCommand(String[] lines, PrintWriter out) {
        String key = lines[6];
        String value = switch (key) {
            case "dir" -> protocolParser.getDir();
            case "dbfilename" -> protocolParser.getDbfilename();
            default -> null;
        };
        responseList(out, key, value);
    }

    private void handleKeysCommand(PrintWriter out) {
        String[] keys = dataStore.getAll().keySet().toArray(new String[0]);
        responseList(out, keys);
    }

    private void response(PrintWriter out, String value) {
        out.print("$" + value.length() + "\r\n");
        out.print(value + "\r\n");
    }

    private void responseList(PrintWriter out, String... values) {
        out.print("*" + values.length + "\r\n");
        for (String value : values) {
            out.print("$" + value.length() + "\r\n");
            out.print(value + "\r\n");
        }
    }

    private String[] readLines(BufferedReader in) throws IOException {
        String firstLine = in.readLine();
        if (firstLine != null) {
            int elements = 1 + 2 * Integer.parseInt(firstLine.substring(1));
            String[] lines = new String[elements];
            lines[0] = firstLine;
            for (int i = 1; i < elements; i++) {
                lines[i] = in.readLine();
            }
            return lines;
        }
        return null;
    }
}
