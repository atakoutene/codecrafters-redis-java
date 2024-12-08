// src/main/java/Master.java
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Master {
    private static final Logger logger = Logger.getLogger(Master.class.getName());
    private static final String REPLICATION_ID = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
    private static final long REPLICATION_OFFSET = 0;
    private final int port;
    private final ExecutorService threadPool;

    public Master(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    public void start() {
        System.out.println("Server started on port: " + port);

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
}