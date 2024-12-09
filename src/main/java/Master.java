// src/main/java/Master.java
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Master {
    private static final Logger logger = Logger.getLogger(Master.class.getName());
    private static final String REPLICATION_ID = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
    private static final long REPLICATION_OFFSET = 0;
    private final int port;
    private final ExecutorService threadPool;

    private static final List<Integer> slavesPort = new ArrayList<>();

    public Master(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    public void start() {
        logger.info("Master started on port: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                final Socket clientSocket = serverSocket.accept();
                ClientTask clientTask = new ClientTask(clientSocket);
                threadPool.submit(clientTask);
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    public static String getReplicationId() {
        return REPLICATION_ID;
    }

    public static long getReplicationOffset() {
        return REPLICATION_OFFSET;
    }

    public static void addSlavePort(int port) {
        slavesPort.add(port);
    }

    public List<Integer> getSlavesPort() {
        return slavesPort;
    }

    public static String handleReplconfCommand(String[] parts) {
        if (parts[1].equalsIgnoreCase("listening-port")) {
            int port = Integer.parseInt(parts[3]);
            addSlavePort(port);
            return "+OK\r\n";
        } else if (parts[1].equalsIgnoreCase("capa") && parts[3].equalsIgnoreCase("psync2")) {
            return "+OK\r\n";
        }
        return "-ERR unknown REPLCONF command\r\n";
    }

    public static String handlePsyncCommand(String[] parts, OutputStream out) throws IOException {
        if (parts[1].equals("?") && parts[3].equals("-1")) {
            String fullResyncResponse = "+FULLRESYNC " + REPLICATION_ID + " 0\r\n";
            out.write(fullResyncResponse.getBytes());
            out.flush();

            // Hex representation of an empty RDB file
            String rdbHex = "524544495330303131fa0972656469732d76657205372e322e30fa0a72656469732d62697473c040fa056374696d65c26d08bc65fa08757365642d6d656dc2b0c41000fa08616f662d62617365c000fff06e3bfec0ff5aa2";
            byte[] rdbBytes = hexStringToByteArray(rdbHex);
            String rdbFile = "$" + rdbBytes.length + "\r\n" + new String(rdbBytes) + "\r\n";
            out.write(rdbFile.getBytes());
            out.flush();

            return null;
        }
        return "-ERR unknown PSYNC command\r\n";
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}