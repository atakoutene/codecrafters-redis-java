// src/main/java/ClientTask.java
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientTask implements Runnable {
    private final Socket clientSocket;
    private final ProtocolParser protocolParser;
    private static final Logger logger = Logger.getLogger(ClientTask.class.getName());

    public ClientTask(Socket clientSocket, ProtocolParser protocolParser) {
        this.clientSocket = clientSocket;
        this.protocolParser = protocolParser;
    }

    @Override
    public void run() {
        logger.info("Client task started for " + clientSocket.getInetAddress());
        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {
            byte[] bytes = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(bytes)) != -1) {
                String command = new String(bytes, 0, bytesRead);
                logger.info("Received command: " + command);
                out.write(protocolParser.parse(command).getBytes());
                out.flush();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException occurred", e);
        } finally {
            try {
                clientSocket.close();
                logger.info("Client socket closed");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "IOException occurred while closing client socket", e);
            }
        }
    }
}