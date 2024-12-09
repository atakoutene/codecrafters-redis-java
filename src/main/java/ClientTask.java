import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            String command;
            while ((command = in.readLine()) != null) {
                String response = ProtocolParser.parse(command, out);
                if (response != null) {
                    out.write(response.getBytes());
                    out.flush();
                }

                if (isWriteCommand(command)) {
                    master.propagateCommand(command);
                }
            }
        } catch (IOException e) {
            logger.severe("Error handling client connection: " + e.getMessage());
        }
    }

    private boolean isWriteCommand(String command) {
        return command.startsWith("SET") || command.startsWith("DEL");
    }
}