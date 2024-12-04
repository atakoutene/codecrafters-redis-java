import java.io.IOException;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private  static DataStore dataStore = new DataStore();  // Initialize DataStore here
    private  static ProtocolParser protocolParser = new ProtocolParser(dataStore, "", "");  // Initialize ProtocolParser here

    public static void main(String[] args) {
        if (args.length > 3) {
            String dir = args[1];
            String dbfilename = args[3];
            try {
                // Parse the data file
                protocolParser.setDir(dir);
                protocolParser.setDbfilename(dbfilename);
                protocolParser.parseFile();
            } catch (IOException e) {
                logger.severe("Error occurred while parsing the data file: " + e.getMessage());
            }
        }

        try {
            // Initialize the server
            Server server = new Server(dataStore, protocolParser);
            server.start();
        } catch (IOException e) {
            logger.severe("Error occurred while starting the server: " + e.getMessage());
        }
    }
}
