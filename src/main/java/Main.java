// src/main/java/Main.java
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Config config = new Config();

        // Parse system arguments
        for (int i = 0; i < args.length; i++) {
            if ("--dir".equals(args[i]) && i + 1 < args.length) {
                config.setConfig("dir", args[i + 1]);
            } else if ("--dbfilename".equals(args[i]) && i + 1 < args.length) {
                config.setConfig("dbfilename", args[i + 1]);
            }
        }

        Cache cache = new Cache();
        ProtocolParser parser = new ProtocolParser(cache, config);
        logger.info("Server is starting...");

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        ExecutorService executor = null;
        int threadPoolSize = 10;
        int port = 6379;

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            executor = Executors.newFixedThreadPool(threadPoolSize);

            while (true) {
                clientSocket = serverSocket.accept();
                logger.info("Accepted connection from " + clientSocket.getInetAddress());
                executor.submit(new ClientTask(clientSocket, parser));
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException occurred", e);
        } finally {
            try {
                if (executor != null) {
                    executor.shutdown();
                }
                if (serverSocket != null) {
                    serverSocket.close();
                }
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "IOException occurred during cleanup", e);
            }
        }
    }
}