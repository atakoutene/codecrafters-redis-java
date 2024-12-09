import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
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
        try (Socket masterSocket = new Socket(masterHost, masterPort);
             OutputStream out = masterSocket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()))) {

            logger.info("Connected to master at " + masterHost + ":" + masterPort);

            // Send PING command
            out.write("*1\r\n$4\r\nPING\r\n".getBytes());
            out.flush();
            String response = in.readLine();
            logger.info("Received response from master: " + response);

            // Send REPLCONF listening-port command
            String replconfListeningPort = "*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n6380\r\n";
            out.write(replconfListeningPort.getBytes());
            out.flush();
            response = in.readLine();
            logger.info("Received response from master: " + response);

            // Send REPLCONF capa psync2 command
            String replconfCapaPsync2 = "*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n";
            out.write(replconfCapaPsync2.getBytes());
            out.flush();
            response = in.readLine();
            logger.info("Received response from master: " + response);

            // Send PSYNC command
            String psyncCommand = "*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n";
            out.write(psyncCommand.getBytes());
            out.flush();
            response = in.readLine();
            logger.info("Received response from master: " + response);

            // Process commands from master
            while ((response = in.readLine()) != null) {
                ProtocolParser.parse(response, null);
            }

        } catch (IOException e) {
            logger.severe("Error connecting to master: " + e.getMessage());
        }
    }
}