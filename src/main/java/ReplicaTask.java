import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;

public class ReplicaTask implements Runnable {
    private final Socket clientSocket;
    private final Replica replica;
    private static final Logger logger = Logger.getLogger(ClientTask.class.getName());

    public ReplicaTask(Socket clientSocket, Replica replica) {
        this.clientSocket = clientSocket;
        this.replica = replica;
    }

    @Override
    public void run() {
        logger.info("Client task started for " + clientSocket.getInetAddress());
        try (OutputStream out = clientSocket.getOutputStream();
             InputStream in = clientSocket.getInputStream()) {

            byte[] bytes = new byte[1024];
            int bytesRead;

            while ((bytesRead = in.read(bytes)) != -1) {
                String command = new String(bytes, 0, bytesRead);
                logger.info("Received command: " + command);
                String response =ProtocolParser.parse(command, out);
                if (response != null) {
                    out.write(response.getBytes());
                    out.flush();
                }
            }

        } catch (IOException e) {
            logger.severe("IOException: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                logger.severe("Failed to close socket: " + e.getMessage());
            }
        }
    }
}