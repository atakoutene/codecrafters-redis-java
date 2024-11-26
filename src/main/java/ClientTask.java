import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientTask implements Runnable{
    private final Socket clientSocket;

    public ClientTask(Socket clientSocket){
        this.clientSocket = clientSocket;
    }
    @Override
    public void run() {
        InputStream in = null;
        OutputStream out = null;
        ProtocolParser protocolParser;
        try{
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
            byte[] bytes = new byte[1024];
            int bytesRead;
            while((bytesRead = in.read(bytes)) != -1) {
                String command = new String(bytes, 0, bytesRead);
                protocolParser = new ProtocolParser(command);
//                System.out.println("Command: " + command);
                out.write(protocolParser.parse().getBytes());
                out.flush();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
