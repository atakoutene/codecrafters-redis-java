import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Main {

    private static final int PORT = 6379;
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        System.out.println("Server started on port: " + PORT);

        // Check if the number of arguments is correct
        if (args.length > 3) {
            // Get the dir and dbfilename parameters
            String dir = args[1];
            String dbfilename = args[3];

            // Log the two parameters
            logger.config("dir: " + dir);
            logger.config("dbfilename: " + dbfilename);

            System.out.println("Reading RDB file: " + dbfilename + " in directory: " + dir);

            // Set the parameters in the persistent storage
            Config.getInstance().setConfig("dir", dir);
            Config.getInstance().setConfig("dbfilename", dbfilename);

            // Read the RDB file
            try {
                RDBReader.readRDBFile(dir, dbfilename);
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
        }


        ServerSocket serverSocket = null;

        // Threadpool to handle multiple clients
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        try {
            // Create a new server socket
            serverSocket = new ServerSocket(PORT);
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
