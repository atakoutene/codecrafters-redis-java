import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final RedisServer redisServer = new RedisServer();

    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

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
                executor.submit(new ClientTask(clientSocket, redisServer));
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
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
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}