import java.io.IOException;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        boolean isReplica = false;
        String masterHost = null;
        int masterPort = 0;
        int port = 6379;

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
                } else if (arg.equals("--port")) {
                    port = Integer.parseInt(value);
                }
                processArgument(arg, value);
            }
        }

        String dbFileName = Config.getInstance().getConfig("dbfilename");
        String dir = Config.getInstance().getConfig("dir");
        if (dbFileName != null && dir != null) {
            try {
                RDBReader.readRDBFile(dir, dbFileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (isReplica) {
            Replica replica = new Replica(masterHost, masterPort);
            replica.start();
        } else {
            Master master = new Master(port);
            master.start();
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