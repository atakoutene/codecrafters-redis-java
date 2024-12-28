package Redis;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class Replica {
    private static final Logger logger = Logger.getLogger(Replica.class.getName());
    private final String masterHost;
    private final int masterPort;
    private final int port;

    public Replica(String masterHost, int masterPort, int port) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            logger.info("Replica started on port: " + port);

            // Connect to the master
            new Thread(this::connectToMaster).start();

            // Accept client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientTask(clientSocket, new Master(port))).start();
            }

        } catch (IOException e) {
            logger.severe("Error starting replica server: " + e.getMessage());
        }
    }

    private void connectToMaster() {
        while (true) {
            try (Socket masterSocket = new Socket(masterHost, masterPort)) {
                logger.info("Connected to master at " + masterHost + ":" + masterPort);
                ReplicaHandler replicaHandler = new ReplicaHandler(masterSocket);
                replicaHandler.start();
            } catch (IOException e) {
                logger.severe("Error connecting to master: " + e.getMessage());
                try {
                    // Wait before retrying to connect
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}