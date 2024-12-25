import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

public class ClientTask implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientTask.class.getName());
    private final Socket clientSocket;
    private final Master master;

    public ClientTask(Socket clientSocket, Master master) {
        this.clientSocket = clientSocket;
        this.master = master;
    }

    @Override
    public void run() {
        logger.info("Client task started for " + clientSocket.getRemoteSocketAddress());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            String line;
            while ((line = in.readLine()) != null) {
                logger.info("Received command: " + line);
                String command = line.trim();
                if (isWriteCommand(command)){
                    logger.info("Write command: " + command);
                    master.propagateCommand(command);
                }
                out.write("+OK\r\n".getBytes());
                out.flush();
            }
        } catch (IOException e) {
            logger.severe("IOException: " + e.getMessage());
        }
    }


    private boolean isWriteCommand(String command) {
        return command.contains("SET") || command.contains("DEL");
    }
}