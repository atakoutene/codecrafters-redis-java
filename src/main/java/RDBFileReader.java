import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

public class RDBFileReader {
    private static final Logger logger = Logger.getLogger(RDBFileReader.class.getName());
    private final Cache cache;

    public RDBFileReader(Cache cache) {
        this.cache = cache;
    }

    private void readDatabaseSection(InputStream inputStream) throws IOException {
        int b;
        while ((b = inputStream.read()) != -1) {
            if (b == 0xFB) {
                // Read the hash table sizes
                int keyValueSize = readSize(inputStream);
                int expirySize = readSize(inputStream);

                // Read key-value pairs
                for (int i = 0; i < keyValueSize + expirySize; i++) {
                    readKeyValuePair(inputStream);
                }
            } else if (b == 0xFF) {
                // End of file section
                readEndOfFile(inputStream);
                break;
            } else {
                throw new IOException("Unexpected byte in database section: " + b);
            }
        }
        logger.info("Read database section");
    }

    private void readKeyValuePair(InputStream inputStream) throws IOException {
        long expiry = 0;
        boolean hasExpiry = false;

        // Check for optional expiry information
        inputStream.mark(1);
        int expiryType = inputStream.read();
        if (expiryType == 0xFC) {
            expiry = readLong(inputStream);
            hasExpiry = true;
        } else if (expiryType == 0xFD) {
            expiry = readInt(inputStream);
            hasExpiry = true;
        } else {
            inputStream.reset();
        }

        int valueType = inputStream.read();
        if (valueType != 0x00) {
            throw new IOException("Unsupported value type: " + valueType);
        }

        String key = readString(inputStream);
        String value = readString(inputStream);

        if (hasExpiry) {
            cache.setWithExpiry(key, value, expiry);
        } else {
            cache.set(key, value);
        }
        logger.info("Loaded key: " + key + ", Value: " + value + ", Expiry: " + (hasExpiry ? expiry : "none"));
    }

    private void readEndOfFile(InputStream inputStream) throws IOException {
        int b = inputStream.read();
        if (b != 0xFF) {
            throw new IOException("Expected end of file marker, but found: " + b);
        }

        byte[] checksum = new byte[8];
        if (inputStream.read(checksum) != 8) {
            throw new IOException("Invalid RDB checksum");
        }
        logger.info("Checksum validated");
    }

    private String readString(InputStream inputStream) throws IOException {
        int length = readSize(inputStream);
        byte[] data = new byte[length];
        if (inputStream.read(data) != length) {
            throw new IOException("Failed to read string");
        }
        return new String(data);
    }

    private int readSize(InputStream inputStream) throws IOException {
        int firstByte = inputStream.read();
        if (firstByte == -1) throw new EOFException();

        int type = (firstByte & 0xC0) >> 6;
        int value = firstByte & 0x3F;

        switch (type) {
            case 0x00:
                return value;
            case 0x01:
                int secondByte = inputStream.read();
                if (secondByte == -1) throw new EOFException();
                return (value << 8) | secondByte;
            case 0x02:
                byte[] longBytes = new byte[4];
                longBytes[3] = (byte) value; // Remaining bits from the first byte
                if (inputStream.read(longBytes, 0, 3) != 3) {
                    throw new IOException("Failed to read size-encoded value");
                }
                return ByteBuffer.wrap(longBytes).order(ByteOrder.BIG_ENDIAN).getInt();
            case 0x03:
                throw new UnsupportedEncodingException("String encoding not supported in size");
            default:
                throw new IOException("Invalid size encoding type");
        }
    }

    private int readInt(InputStream inputStream) throws IOException {
        byte[] intBytes = new byte[4];
        if (inputStream.read(intBytes) != 4) {
            throw new IOException("Failed to read int");
        }
        return ByteBuffer.wrap(intBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private long readLong(InputStream inputStream) throws IOException {
        byte[] longBytes = new byte[8];
        if (inputStream.read(longBytes) != 8) {
            throw new IOException("Failed to read long");
        }
        return ByteBuffer.wrap(longBytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }
}