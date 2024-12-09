// src/main/java/Main.java
import java.io.IOException;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        boolean isReplica = false;
        String masterHost = null;
        int masterPort = 0;
        for (int i = 0; i < args.length; i++) {
            logger.config("args[" + i + "]: " + args[i]);
            if (args[i].startsWith("--")) {
                String arg = args[i];
                String value = args[i + 1];
                if (arg.equals("--replicaof")) {
                    isReplica = true;
                    String[] masterInfo = value.split(" ");
                    masterHost = masterInfo[0];
                    masterPort = Integer.parseInt(masterInfo[1]);
                }
                processArgument(arg, value);
            }
        }
        String portLiteral = Config.getInstance().getConfig("port");
        int port = Integer.parseInt(portLiteral != null ? portLiteral : "6379");
        //RDB file execution
        String dbFileName = Config.getInstance().getConfig("dbfilename");
        String dir = Config.getInstance().getConfig("dir");
        if (dbFileName != null && dir != null) {
            try {
                RDBReader.readRDBFile(dir, dbFileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        //start the server
        //always create a master instance in case of no replica
        Master master = new Master(port);
        master.start();
        //start the master or replicas
        if (isReplica) {
            Replica replica = new Replica(masterHost, masterPort);
            replica.start();
        }

    }

    private static void processArgument(String arg, String value) {
        switch (arg) {
            case "--port":
                Config.getInstance().setConfig("port", value);
                break;
            case "--dir":
                Config.getInstance().setConfig("dir", value);
                break;
            case "--dbfilename":
                Config.getInstance().setConfig("dbfilename", value);
                break;
            case "--replicaof":
                Config.getInstance().setConfig("replicaof", value);
                break;
            default:
                logger.warning("Unknown argument: " + arg);
                break;
        }
    }
}