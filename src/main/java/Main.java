import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        int port = (args.length > 1 & args.length < 3) ? Integer.parseInt(args[1]) : 6379;
        System.out.println("Server started on port: " + port);

        // Check if the number of arguments is correct
        if (args.length > 3) {
            // Get the dir and dbFileName parameters
            String dir = args[1];
            String dbFileName = args[3];

            // Log the two parameters
            logger.config("dir: " + dir);
            logger.config("dbFileName: " + dbFileName);

            System.out.println("Reading RDB file: " + dbFileName + " in directory: " + dir);

            // Set the parameters in the persistent storage
            Config.getInstance().setConfig("dir", dir);
            Config.getInstance().setConfig("dbFileName", dbFileName);

            // Read the RDB file
            try {
                RDBReader.readRDBFile(dir, dbFileName);
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
        }


        ServerSocket serverSocket = null;

        // Threadpool to handle threads concurrently efficiently
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        try {
            // Create a server socket
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);

            // keep server running
            while (true) {
                final Socket clientSocket = serverSocket.accept();

                // Span a new thread to handle the new client
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
