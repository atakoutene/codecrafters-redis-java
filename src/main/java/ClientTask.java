import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientTask implements Runnable {
    private final Socket clientSocket;
    private final ProtocolParser protocolParser;
    private final DataStore dataStore;  // Direct reference to DataStore
    private static final Logger logger = Logger.getLogger(ClientTask.class.getName());

    public ClientTask(Socket clientSocket, ProtocolParser protocolParser, DataStore dataStore) {
        this.clientSocket = clientSocket;
        this.protocolParser = protocolParser;
        this.dataStore = dataStore;  // Initialize DataStore here
    }

    @Override
    public void run() {
        logger.info("Client task started for " + clientSocket.getInetAddress());
        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            byte[] bytes = new byte[1024];
            int bytesRead;

            while ((bytesRead = in.read(bytes)) != -1) {
                String command = new String(bytes, 0, bytesRead);  // Read command from client
                logger.info("Received command: " + command);

                // Handle command
                String response = handleCommand(command);
                out.write(response.getBytes());  // Send the response back to client
                out.flush();  // Ensure response is sent
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException occurred while processing client request", e);
        } finally {
            try {
                clientSocket.close();
                logger.info("Client socket closed");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "IOException occurred while closing client socket", e);
            }
        }
    }

    private String handleCommand(String command) {
        // Here you can parse the command and interact with the dataStore
        // For example, assuming command structure like "GET key" or "SET key value"

        String[] parts = command.split(" ", 2);
        String action = parts[0];

        if ("GET".equals(action)) {
            // Handle GET command (fetch value from the DataStore)
            if (parts.length == 2) {
                String key = parts[1];
                String value = dataStore.get(key);  // Direct access to DataStore
                return value != null ? value : "KEY NOT FOUND";
            } else {
                return "ERROR: Missing key for GET command";
            }
        } else if ("SET".equals(action)) {
            // Handle SET command (store key-value in the DataStore)
            if (parts.length == 3) {
                String key = parts[1];
                String value = parts[2];
                dataStore.put(key, value, null);  // Store key-value pair directly in DataStore
                return "OK";
            } else {
                return "ERROR: Missing key or value for SET command";
            }
        } else {
            return "ERROR: Unknown command";
        }
    }
}
