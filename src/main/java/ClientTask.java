import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientTask implements Runnable {
    private final Socket clientSocket;
    private final ProtocolParser protocolParser;

    public ClientTask(Socket clientSocket, RedisServer redisServer) {
        this.clientSocket = clientSocket;
        this.protocolParser = new ProtocolParser(redisServer);
    }

    @Override
    public void run() {
        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {
            byte[] bytes = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(bytes)) != -1) {
                String command = new String(bytes, 0, bytesRead);
                out.write(protocolParser.parse(command).getBytes());
                out.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}