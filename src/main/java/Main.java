// src/main/java/Main.java
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        int port = 6379;
        boolean isReplica = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
            } else if (args[i].equals("--replicaof") && i + 2 < args.length) {
                isReplica = true;
                // You can store the master host and port if needed
                String masterHost = args[i + 1];
                int masterPort = Integer.parseInt(args[i + 2]);
                logger.config("Replica of: " + masterHost + ":" + masterPort);
            }
        }

        Config.getInstance().setReplica(isReplica);

        System.out.println("Server started on port: " + port);

        ServerSocket serverSocket = null;
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        try {
            serverSocket = new ServerSocket(port);
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
}