package client;

import models.Torrent;
import protos.Proto;
import utils.Connection;
import utils.ConnectionException;
import utils.Helper;
import utils.Node;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static client.ClientInit.initLibrary;

/**
 * @author Alberto Delgado on 5/9/22
 * @author anchit bhatia
 * @project dsd-final-project-anchitbhatia
 * <p>
 * Main client node. Can act as a seeder or leecher or both. Will
 * join "swarm" on boot up and send heartbeats to tracker to let it
 * know it is alive.
 */
public class Client extends Node {
    Library library;
    Connection trackerConnection;
    HeartbeatManager heartbeatManager;
    Map<String, FileDownloader> downloads = new HashMap<>();
    boolean testing = false;

    public Client(String hostname, String ip, int port) throws IOException {
        super(hostname, ip, port);
        initializeServer(new PeerServer());
        library = initLibrary();
    }

    /**
     * Initiates local server plus joins swarm and inits heartbeats
     */
    @Override
    public void startServer() {
        super.startServer();
        try {
            trackerConnection = ClientInit.joinSwarm(hostname, ip, port);
            heartbeatManager = new HeartbeatManager(hostname, ip, port);
            heartbeatManager.init(trackerConnection);
        } catch (ConnectionException ignored) {
            // ignore for the time being
        }
    }

    /**
     * Stop server and heartbeats
     */
    @Override
    public void stopServer() {
        super.stopServer();
        heartbeatManager.stop();
    }

    /**
     * Starts downloading a file following
     * torrent details
     *
     * @param torrent
     */
    public void downloadFile(Torrent torrent) {
        FileDownloader downloader = new FileDownloader(this, torrent);
        if (testing) downloader.testing();
        downloads.put(torrent.name, downloader);
        new Thread(downloader).start();
    }

    /**
     * Notifies tracker of new pieces availability
     *
     * @param torrent      torrent
     * @param pieceNumbers piece numbers available
     * @throws ConnectionException if it cannot connect
     */
    public void notifyTracker(Torrent torrent, List<Long> pieceNumbers) throws ConnectionException {
        Proto.Request request = Proto.Request.newBuilder().
                setNode(Helper.getNodeDetailsObject(this)).
                setRequestType(Proto.Request.RequestType.SEED_PIECE).
                setFileName(torrent.name).
                addAllPieceNumbers(pieceNumbers).
                build();
        trackerConnection.send(request.toByteArray());
    }

    /**
     * For testing purposes. Sets the client in testing mode
     *
     * @return return client
     */
    public Client testing() {
        testing = true;
        library = initLibrary(true);
        return this;
    }

    /**
     * Handles pool connections
     */
    private class PeerServer implements Runnable {
        private final ExecutorService peerConnectionPool;

        public PeerServer() {
            this.peerConnectionPool = Executors.newCachedThreadPool();
        }

        /**
         * Run Server
         */
        @Override
        public void run() {
            try {
                System.out.println("Starting server on clientNode at " + port);
                while (isServerRunning) {
                    Socket clientSocket = serverSocket.accept();
                    Connection connection = new Connection(clientSocket);
                    this.peerConnectionPool.execute(new ConnectionHandler(library, connection));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
