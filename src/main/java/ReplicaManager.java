//import java.io.IOException;
//import java.io.OutputStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.logging.Logger;
//
//public class ReplicaManager {
//    private static final Logger logger = Logger.getLogger(ReplicaManager.class.getName());
//    private final List<OutputStream> replicaStreams = new ArrayList<>();
//    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
//
//    public synchronized void registerReplica(OutputStream out) {
//        replicaStreams.add(out);
//        logger.info("Registered new replica. Total replicas: " + replicaStreams.size());
//    }
//
//    public void propagateCommand(String command) {
//        threadPool.submit(() -> {
//            synchronized (replicaStreams) {
//                for (OutputStream out : replicaStreams) {
//                    try {
//                        out.write(command.getBytes());
//                        out.flush();
//                    } catch (IOException e) {
//                        logger.severe("Failed to send command to a replica: " + e.getMessage());
//                    }
//                }
//            }
//        });
//    }
//
//    public void shutdown() {
//        threadPool.shutdown();
//    }
//}