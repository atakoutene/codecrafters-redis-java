import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ProtocolParser {
    private DataStore dataStore;
    private String dir;
    private String dbfilename;

    public ProtocolParser(DataStore dataStore, String dir, String dbfilename) {
        this.dataStore = dataStore;
        this.dir = dir;
        this.dbfilename = dbfilename;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public void setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getDbfilename() {
        return dbfilename;
    }

    public void setDbfilename(String dbfilename) {
        this.dbfilename = dbfilename;
    }

    //read the file and parse the data into the dataStore
    public void parseFile() throws IOException {
        File file = new File(dir, dbfilename);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                int b;
                while ((b = in.read()) != 0xff) {
                    if (b == 0xFB) {
                        getLength(in); // skip lengths
                        getLength(in); // skip lengths
                        break;
                    }
                }
                while ((b = in.read()) != -1) {
                    int valueType = b;
                    Long expiry = null;
                    if (b == 0xFC) {
                        expiry = readExpiry(in);
                    }

                    String key = readString(in);
                    String value = readString(in);

                    dataStore.put(key, value, expiry);
                }
            }
        }
    }

    private Long readExpiry(InputStream in) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(in.readNBytes(Long.BYTES));
        buffer.flip();
        return buffer.getLong();
    }

    private String readString(InputStream in) throws IOException {
        int length = getLength(in);
        byte[] bytes = in.readNBytes(length);
        return new String(bytes);
    }

    private int getLength(InputStream in) throws IOException {
        int length = 0;
        byte b = (byte) in.read();
        switch (b & 0b11000000) {
            case 0:
                length = b & 0b00111111;
                break;
            case 128:
                ByteBuffer buffer = ByteBuffer.allocate(2);
                buffer.put((byte) (b & 0b00111111));
                buffer.put((byte) in.read());
                buffer.rewind();
                length = buffer.getShort();
                break;
            case 256:
                buffer = ByteBuffer.allocate(4);
                buffer.put(b);
                buffer.put(in.readNBytes(3));
                buffer.rewind();
                length = buffer.getInt();
                break;
            case 384:
                System.out.println("Special format");
                break;
        }
        return length;
    }
}
