import java.io.*;
import java.util.concurrent.TimeUnit;

public class RDBFileReader {

    public static void readRDBFile(String dir, String filename) throws IOException {
        File file = new File(dir, filename);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            int index = 0;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            byte[] fileContent = baos.toByteArray();
            index = 0;
            int fileLength = fileContent.length;

            // Skip the header section
            index += 9; // "REDIS0011"

            while (index < fileLength) {
                byte firstByte = fileContent[index];

                if (firstByte == (byte) 0xFA) {
                    // Skip metadata section
                    index = skipMetadataSection(fileContent, index);
                } else if (firstByte == (byte) 0xFE) {
                    // Skip database section header
                    index += 2;
                } else if (firstByte == (byte) 0xFB) {
                    // Read key-value pairs
                    index = readKeyValuePair(fileContent, index);
                } else if (firstByte == (byte) 0xFF) {
                    // End of file section
                    break;
                } else {
                    index++;
                }
            }
        }
    }

    private static int skipMetadataSection(byte[] buffer, int index) {
        index++; // Skip FA
        index += readSizeEncodedValue(buffer, index); // Skip metadata name
        index += readSizeEncodedValue(buffer, index); // Skip metadata value
        return index;
    }

    private static int readKeyValuePair(byte[] buffer, int index) {
        long expireTimestamp = -1;
        boolean isMilliseconds = false;

        if (buffer[index] == (byte) 0xFC) {
            isMilliseconds = true;
            index++;
            expireTimestamp = readLong(buffer, index);
            index += 8;
        } else if (buffer[index] == (byte) 0xFD) {
            index++;
            expireTimestamp = readInt(buffer, index);
            index += 4;
        }

        byte valueType = buffer[index];
        index++;

        int keyLength = readSizeEncodedValue(buffer, index);
        index += getSizeEncodedLength(buffer[index]);

        String key = new String(buffer, index, keyLength);
        index += keyLength;

        int valueLength = readSizeEncodedValue(buffer, index);
        index += getSizeEncodedLength(buffer[index]);

        String value = new String(buffer, index, valueLength);
        index += valueLength;

        if (!key.isEmpty() && !value.isEmpty()) {
            if (expireTimestamp != -1) {
                Cache.getInstance().setWithExpiry(key, value, expireTimestamp, isMilliseconds ? TimeUnit.MILLISECONDS : TimeUnit.SECONDS);
            } else {
                Cache.getInstance().set(key, value);
            }
        }

        return index;
    }

    private static long readLong(byte[] buffer, int index) {
        return ((buffer[index] & 0xFFL)) |
                ((buffer[index + 1] & 0xFFL) << 8) |
                ((buffer[index + 2] & 0xFFL) << 16) |
                ((buffer[index + 3] & 0xFFL) << 24) |
                ((buffer[index + 4] & 0xFFL) << 32) |
                ((buffer[index + 5] & 0xFFL) << 40) |
                ((buffer[index + 6] & 0xFFL) << 48) |
                ((buffer[index + 7] & 0xFFL) << 56);
    }

    private static int readInt(byte[] buffer, int index) {
        return ((buffer[index] & 0xFF)) |
                ((buffer[index + 1] & 0xFF) << 8) |
                ((buffer[index + 2] & 0xFF) << 16) |
                ((buffer[index + 3] & 0xFF) << 24);
    }

    private static int readSizeEncodedValue(byte[] buffer, int index) {
        byte firstByte = buffer[index];
        int size;

        if ((firstByte & 0xC0) == 0x00) {
            size = firstByte & 0x3F;
        } else if ((firstByte & 0xC0) == 0x40) {
            size = ((firstByte & 0x3F) << 8) | (buffer[index + 1] & 0xFF);
        } else if ((firstByte & 0xC0) == 0x80) {
            size = ((buffer[index + 1] & 0xFF) << 24) | ((buffer[index + 2] & 0xFF) << 16) |
                    ((buffer[index + 3] & 0xFF) << 8) | (buffer[index + 4] & 0xFF);
        } else {
            size = handleStringEncoding(buffer, index);
        }

        return size;
    }

    private static int handleStringEncoding(byte[] buffer, int index) {
        throw new IllegalArgumentException("String encoding type not supported in this example");
    }

    private static int getSizeEncodedLength(byte firstByte) {
        if ((firstByte & 0xC0) == 0x00) {
            return 1;
        } else if ((firstByte & 0xC0) == 0x40) {
            return 2;
        } else if ((firstByte & 0xC0) == 0x80) {
            return 5;
        } else {
            return 1;
        }
    }
}