import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
                new Thread(new ClientTask(clientSocket, null)).start(); // Pass null as master since it's a replica
            }

        } catch (IOException e) {
            logger.severe("Error starting replica server: " + e.getMessage());
        }
    }

    private void connectToMaster() {
        while (true) {
            try (Socket masterSocket = new Socket(masterHost, masterPort);
                 OutputStream out = masterSocket.getOutputStream();
                 InputStream in = masterSocket.getInputStream()) {

                logger.info("Connected to master at " + masterHost + ":" + masterPort);

                // Send PING command
                out.write("*1\r\n$4\r\nPING\r\n".getBytes());
                out.flush();
                byte[] buffer = new byte[1024];
                int bytesRead = in.read(buffer);
                String response = new String(buffer, 0, bytesRead);
                logger.info("Received response from master: " + response);

                // Send REPLCONF listening-port command
                String replconfListeningPort = String.format("*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$%d\r\n%d\r\n", String.valueOf(port).length(), port);
                out.write(replconfListeningPort.getBytes());
                out.flush();
                bytesRead = in.read(buffer);
                response = new String(buffer, 0, bytesRead);
                logger.info("Received response from master: " + response);

                // Send REPLCONF capa psync2 command
                String replconfCapaPsync2 = "*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n";
                out.write(replconfCapaPsync2.getBytes());
                out.flush();
                bytesRead = in.read(buffer);
                response = new String(buffer, 0, bytesRead);
                logger.info("Received response from master: " + response);

                // Send PSYNC command
                String psyncCommand = "*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n";
                out.write(psyncCommand.getBytes());
                out.flush();
                bytesRead = in.read(buffer);
                response = new String(buffer, 0, bytesRead);
                logger.info("Received response from master: " + response);

                // Process FULLRESYNC and RDB file
                StringBuilder commandBuffer = new StringBuilder();
                while ((bytesRead = in.read(buffer)) != -1) {
                    commandBuffer.append(new String(buffer, 0, bytesRead));
                    String[] commands = commandBuffer.toString().split("\r\n\r\n");
                    for (int i = 0; i < commands.length - 1; i++) {
                        String command = commands[i] + "\r\n";
                        logger.info("Received command from master: " + command);
                        if (command.startsWith("+FULLRESYNC")) {
                            // Handle FULLRESYNC command
                            logger.info("Handling FULLRESYNC command");
                            // Extract RDB file and process it
                            // Assuming RDB file is received as a bulk string
                            int rdbStartIndex = command.indexOf("\r\n$") + 3;
                            int rdbLength = Integer.parseInt(command.substring(rdbStartIndex, command.indexOf("\r\n", rdbStartIndex)));
                            String rdbData = command.substring(command.indexOf("\r\n", rdbStartIndex) + 2, rdbStartIndex + 2 + rdbLength);
                            // Process RDB data (this is a placeholder, actual RDB processing logic needed)
                            logger.info("Received RDB data of length: " + rdbLength);
                        } else {
                            // Process other commands (e.g., SET commands)
                            ProtocolParser.parse(command, out);
                        }
                    }
                    commandBuffer = new StringBuilder(commands[commands.length - 1]);
                }

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