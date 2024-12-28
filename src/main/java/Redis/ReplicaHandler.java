package Redis;

import java.io.*;
import java.net.Socket;

public class ReplicaHandler {
    private final Socket masterSocket;
    private final Config serverConfig;
    private final Cache storageManager;
    private int offset = 0;

    public ReplicaHandler(Socket masterSocket) {
        this.serverConfig = Config.getInstance();
        this.storageManager = Cache.getInstance();
        this.masterSocket = masterSocket;
    }

    public void start() {
        try {
            OutputStream outputStream = masterSocket.getOutputStream();
            InputStream inputStream = masterSocket.getInputStream();
            sendPing(outputStream);
            readMasterResponse(inputStream);
            sendReplConf(outputStream, Integer.toString(serverConfig.getPort()));
            readMasterResponse(inputStream);
            readMasterResponse(inputStream);
            sendPsync(outputStream);
            readMasterResponse(inputStream);
            skipRdbFile(inputStream);
            Thread cmd = new Thread(() -> listenForCommands(inputStream, outputStream));
            cmd.start();
        } catch (IOException e) {
            System.err.println("Error initiating replica: " + e.getMessage());
        }
    }

    private void sendPing(OutputStream outputStream) throws IOException {
        System.out.println("Sending PING to master...");
        String pingRequest = ProtocolParser.encodeCommand(new String[] {"PING"});
        outputStream.write(pingRequest.getBytes());
    }

    private void sendReplConf(OutputStream outputStream, String port) throws IOException {
        System.out.println("Sending REPL config to master...");
        String firstRequest = ProtocolParser.encodeCommand(new String[] {"REPLCONF", "listening-port", port});
        outputStream.write(firstRequest.getBytes());
        String secondRequest = ProtocolParser.encodeCommand(new String[] {"REPLCONF", "capa", "psync2"});
        outputStream.write(secondRequest.getBytes());
    }

    private void sendPsync(OutputStream outputStream) throws IOException {
        System.out.println("Sending PSYNC to master...");
        String request = ProtocolParser.encodeCommand(new String[] {"PSYNC", "?", "-1"});
        outputStream.write(request.getBytes());
    }

    private void readMasterResponse(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[56];
        int bytesRead = inputStream.read(buffer);

        if (bytesRead > 0) {
            String response = new String(buffer, 0 , bytesRead);
            System.out.println("Received response from master: " + response);
        } else {
            System.out.println("No bytes read from input stream");
        }
    }

    private void skipRdbFile(InputStream inputStream) throws IOException {
        byte[] lengthBuffer = new byte[5];
        inputStream.read(lengthBuffer);
        String lengthPrefix = new String(lengthBuffer);

        if (!lengthPrefix.startsWith("$")) {
            System.out.println("Unexpected RDB length prefix: " + lengthPrefix);
            return;
        }

        int rdbLength = Integer.parseInt(lengthPrefix.substring(1, lengthPrefix.indexOf('\r')));
        byte[] rdbBuffer = new byte[rdbLength];
        inputStream.read(rdbBuffer);
    }

    private void listenForCommands(InputStream inputStream, OutputStream outputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String[] args = ProtocolParser.decodeCommand(reader);
                if (args == null || args.length == 0) {
                    System.err.println("No commands received from master.");
                    break;
                }

                String command = args[0].toUpperCase();
                if ("REPLCONF".equals(command)) {
                    System.out.println("Handling master command...");
                    handleReplConfCommand(args, outputStream);
                } else {
                    System.out.println("Handling propagated command...");
                    handlePropagatedCommand(args);
                }
                offset += ProtocolParser.encodeCommand(args).getBytes().length;
            }
        } catch (IOException e) {
            System.err.println("Error reading commands: " + e.getMessage());
        }
    }

    private void handlePropagatedCommand(String[] args) {
        String command = args[0].toUpperCase();
        switch (command) {
            case "SET":
                if (args.length < 3) {
                    System.err.println("Invalid SET from master, too few arguments");
                    return;
                }
                String key = args[1], value = args[2];
                storageManager.set(key, value);
                break;
            default:
                break;
        }
    }

    private void handleReplConfCommand(String[] args, OutputStream outputStream) throws IOException {
        if ("GETACK".equalsIgnoreCase(args[1])) {
            String offsetStr = Integer.toString(offset);
            String resp = ProtocolParser.encodeCommand(new String[] {"REPLCONF", "ACK", offsetStr});
            outputStream.write(resp.getBytes());
            outputStream.flush();
        } else {
            System.out.println("Unhandled REPLCONF command: " + args[1]);
        }
    }
}