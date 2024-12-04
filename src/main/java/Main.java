import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
public class Main {
    private static String dir;
    private static String dbfilename;
    private static Map<String, String> dataStore = new HashMap<>();
    private static Map<String, Long> expiryStore = new HashMap<>();
    public static void main(String[] args) throws IOException {
        if (args.length == 4) {
            dir = args[1];
            dbfilename = args[3];
            File file = new File(dir, dbfilename);
            if (file.exists()) {
                InputStream in = new FileInputStream(file);
                int b;
                int lengthEncoding;
                int valueType;
                byte[] bytes;
                String key;
                String value;
                // skip
                while ((b = in.read()) != -1) {
                    if (b == 0xFB) {
                        getLength(in);
                        getLength(in);
                        break;
                    }
                }
                while ((b = in.read()) != 0xff) {
                    Long expiry = null;
                    if (b == 0xFC) {
                        System.out.println("ms");
                        ByteBuffer buffer =
                                ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                        buffer.put(in.readNBytes(Long.BYTES));
                        buffer.flip();
                        expiry = buffer.getLong();
                        valueType = in.read();
                    } else {
                        valueType = b;
                    }
                    // Key
                    lengthEncoding = getLength(in);
                    bytes = in.readNBytes(lengthEncoding);
                    key = new String(bytes);
                    // Value
                    lengthEncoding = getLength(in);
                    bytes = in.readNBytes(lengthEncoding);
                    value = new String(bytes);
                    dataStore.put(key, value);
                    // Expiry
                    if (expiry != null) {
                        expiryStore.put(key, expiry);
                    }
                }
            }
        }
        ServerSocket serverSocket = null;
        int port = 6379;
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        // Wait for connection from client.
        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }
    private static void handleClient(Socket clientSocket) {
        try (clientSocket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream());) {
            String[] lines;
            while ((lines = readLines(in)) != null) {
                System.out.println("lines = " + Arrays.toString(lines));
                if (lines[2].equals("ping")) {
                    out.print("+PONG\r\n");
                } else if (lines[2].equals("echo")) {
                    response(out, lines[4]);
                    // out.print(lines[3] + "\r\n");
                    // out.print(lines[4] + "\r\n");
                } else if (lines[2].equals("set")) {
                    String key = lines[4];
                    String value = lines[6];
                    dataStore.put(key, value);
                    if (lines.length == 11) {
                        long expiry =
                                Long.parseLong(lines[10]) + System.currentTimeMillis();
                        expiryStore.put(key, expiry);
                    }
                    out.print("+OK\r\n");
                } else if (lines[2].equals("get")) {
                    String key = lines[4];
                    if (dataStore.containsKey(key)) {
                        String value = dataStore.get(key);
                        if (expiryStore.containsKey(key)) {
                            long expiry = expiryStore.get(key);
                            if (expiry < System.currentTimeMillis()) {
                                dataStore.remove(key);
                                expiryStore.remove(key);
                                value = null;
                            }
                        }
                        if (value == null) {
                            out.print("$-1\r\n");
                        } else {
                            response(out, value);
                            // out.print("$" + value.length() + "\r\n");
                            // out.print(value + "\r\n");
                        }
                    } else {
                        out.print("$-1\r\n");
                    }
                } else if (lines[2].equals("config")) {
                    String key = lines[6];
                    String value = null;
                    if (key.equals("dir")) {
                        value = dir;
                    } else if (key.equals("dbfilename")) {
                        value = dbfilename;
                    }
                    responseList(out, key, value);
                } else if (lines[2].equals("keys")) {
                    String[] keys = dataStore.keySet().toArray(new String[0]);
                    responseList(out, keys);
                }
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
    private static void response(PrintWriter out, String value) {
        out.print("$" + value.length() + "\r\n");
        out.print(value + "\r\n");
    }
    private static void responseList(PrintWriter out, String... values) {
        out.print("*" + values.length + "\r\n");
        for (String value : values) {
            out.print("$" + value.length() + "\r\n");
            out.print(value + "\r\n");
        }
    }
    private static String[] readLines(BufferedReader in) throws IOException {
        String[] lines = null;
        String firstLine = in.readLine();
        if (firstLine != null) {
            int elements = 1 + 2 * Integer.parseInt(firstLine.substring(1));
            lines = new String[elements];
            lines[0] = firstLine;
            for (int i = 1; i < elements; i++) {
                lines[i] = in.readLine();
            }
        }
        return lines;
    }
    private static int getLength(InputStream in) throws IOException {
        int length = 0;
        byte b = (byte)in.read();
        switch (b & 0b11000000) {
            case 0 -> {
                length = b & 0b00111111;
            }
            case 128 -> {
                ByteBuffer buffer = ByteBuffer.allocate(2);
                buffer.put((byte) (b & 00111111));
                buffer.put((byte) in.read());
                buffer.rewind();
                length = buffer.getShort();
            }
            case 256 -> {
                ByteBuffer buffer = ByteBuffer.allocate(4);
                buffer.put(b);
                buffer.put(in.readNBytes(3));
                buffer.rewind();
                length = buffer.getInt();
            }
            case 384 -> {
                System.out.println("Special format");
            }
        }
        return length;
    }
}