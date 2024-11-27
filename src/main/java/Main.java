import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Cache CACHE = new Cache();
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
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
                executor.submit(new ClientTask(clientSocket, CACHE));
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