import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;

public class ClientTask implements Runnable {
    private final Socket clientSocket;

    private final Master master;

    private static final Logger logger = Logger.getLogger(ClientTask.class.getName());

    public ClientTask(Socket clientSocket, Master master) {
        this.clientSocket = clientSocket;
        this.master = master;
    }

    @Override
    public void run() {
        logger.info("Client task started for " + clientSocket.getInetAddress());
        // Open the input and output streams
        try (OutputStream out = clientSocket.getOutputStream();
             InputStream in = clientSocket.getInputStream();)
        {

            // Get the input stream
            byte[] bytes = new byte[1024];
            int bytesRead;

            // Read just bytesRead element
            while ((bytesRead = in.read(bytes)) != -1) {
                // Convert the bytes to a string
                String command = new String(bytes, 0, bytesRead);
                //logger.info("Received command From Task: " + command);

                // Parse the command
                String response = ProtocolParser.parse(command, out);
                if (response != null) {
                    out.write(response.getBytes());
                    out.flush();
                }

                // Check if the command is a write command
                if (isWriteCommand(command)) {
                    logger.info("Propagating command to replicas: " + command);
                    master.propagateCommand(command);
                }
            }

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Failed to close socket: " + e.getMessage());
            }
        }

    }
    private boolean isWriteCommand(String command) {
        return command.contains("SET") || command.contains("DEL");
    }

    }
