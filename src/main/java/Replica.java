// src/main/java/Replica.java
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Logger;

public class Replica {
    private static final Logger logger = Logger.getLogger(Replica.class.getName());
    private final String masterHost;
    private final int masterPort;

    public Replica(String masterHost, int masterPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }

    public void start() {
        try (Socket MasterSocket = new Socket(masterHost, masterPort);
             PrintWriter out = new PrintWriter(MasterSocket.getOutputStream(), true);
             Scanner in = new Scanner(MasterSocket.getInputStream())) {

            logger.info("Connected to master at " + masterHost + ":" + masterPort);

            // Example of sending a command to the master
            out.print("*1\r\n$4\r\nPING\r\n");
            String response = in.nextLine();
            logger.info("Received response from master: " + response);

            // Handle the response from the master
            // (You can add more logic here to handle different commands and responses)

        } catch (IOException e) {
            logger.severe("Error connecting to master: " + e.getMessage());
        }
    }
}