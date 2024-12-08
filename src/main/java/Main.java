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
        for (int i = 0; i < args.length; i++) {
            logger.config("args[" + i + "]: " + args[i]);
            // Process all the arguments
            if (args[i].startsWith("--")) {
                String arg = args[i];
                String value = args[i + 1];
                processArgument(arg, value);
            }
        }

        int port = Integer.parseInt(Config.getInstance().getConfig("port"));

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

    private static void processArgument(String arg, String value) {
        switch (arg) {
            case "--port":
                int port = Integer.parseInt(value);
                logger.config("PORT: " + port);
                Config.getInstance().setConfig("port", Integer.toString(port));
                break;
            case "--dir":
                logger.config("dir: " + value);
                Config.getInstance().setConfig("dir", value);
                break;
            case "--dbfilename":
                logger.config("dbfilename: " + value);
                Config.getInstance().setConfig("dbfilename", value);
                break;
            case "--replicaof":
                logger.config("replicaof: " + value);
                Config.getInstance().setConfig("replicaof", value);
                break;
            default:
                logger.warning("Unknown argument: " + arg);
                break;
        }
    }
}
