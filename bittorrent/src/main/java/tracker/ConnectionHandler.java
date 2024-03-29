package tracker;

import com.google.protobuf.InvalidProtocolBufferException;
import protos.Node.NodeDetails;
import protos.Proto;
import protos.Proto.Request;
import protos.Proto.Request.RequestType;
import protos.Response;
import protos.Response.FileInfo;
import utils.Connection;
import utils.ConnectionException;
import utils.Helper;
import utils.Node;

import java.util.List;
import java.util.Map;

/**
 * @author anchit bhatia
 * @author alberto delgado
 * <p>
 * Handles the logic for each connection in the tracker
 * - Returns peers information on a file
 * - Handles membership
 * - Handles received heartbeat
 * - Handles give piece information
 */
public class ConnectionHandler implements Runnable {
    private final Connection connection;
    private final Tracker tracker;

    public ConnectionHandler(Tracker tracker, Connection connection) {
        this.tracker = tracker;
        this.connection = connection;
    }

    /**
     * Parses received request
     *
     * @return
     */
    private Request receiveRequest() {
        byte[] message = this.connection.receive();
        if (message == null) return null;

        try {
            return Request.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            System.out.println("[TRACKER] Invalid packet received");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Serves peers information
     *
     * @param request
     * @throws ConnectionException
     */
    private void serveRequestPeers(Request request) throws ConnectionException {
        System.out.println("Request received: " + request);
        String fileName = request.getFileName();
        Map<Long, List<NodeDetails>> fileInfo = this.tracker.getFileInfo(fileName);

        System.out.println("fileinfo from database: " + fileInfo);

        FileInfo.Builder responseBuilder = FileInfo.newBuilder();
        if (fileInfo != null) {
            for (Map.Entry<Long, List<NodeDetails>> item : fileInfo.entrySet()) {
                Long pieceNumber = item.getKey();
                Response.PeersList peersList = Response.PeersList.newBuilder().
                        addAllNodes(item.getValue()).
                        build();
                responseBuilder.putPiecesInfo(pieceNumber, peersList);
            }
        } else {
            NodeDetails nullNode = Helper.getNodeDetailsObject(new Node("null", "null", 0));
            Response.PeersList peersList = Response.PeersList.newBuilder().
                    addNodes(nullNode).
                    build();
            responseBuilder.putPiecesInfo(-1, peersList);
        }
        FileInfo response = responseBuilder.build();
        this.connection.send(response.toByteArray());
    }

    /**
     * Handles received heartbeat
     *
     * @param request
     */
    private void handleHeartbeat(Request request) {
        NodeDetails node = request.getNode();
        tracker.heartbeatReceived(node);
    }

    /**
     * Handles new peer membership
     *
     * @param request
     */
    private void addMembership(Request request) {
        System.out.println("Received peer membership request");
        NodeDetails nodeDetails = request.getNode();
        Node peer = Helper.getNodeObject(nodeDetails);

        this.tracker.addPeer(peer, request.getTorrentsList());

        for (Proto.Torrent torrent : request.getTorrentsList()) {
            String fileName = torrent.getFilename();
            System.out.println("Filename: " + torrent.getFilename());
            System.out.println("Pieces list received: " + torrent.getPiecesList());
            for (Long piece : torrent.getPiecesList()) {
                this.tracker.addPieceInfo(fileName, piece, peer);
            }
        }
    }

    /**
     * Retrieves piece information
     *
     * @param request
     */
    private void serveRequestSeedPiece(Request request) {
        System.out.println("Received seed piece request");
        Node node = Helper.getNodeObject(request.getNode());
        String fileName = request.getFileName();
        List<Long> pieceNumbers = request.getPieceNumbersList();
        for (long pieceNumber : pieceNumbers) {
            this.tracker.addPieceInfo(fileName, pieceNumber, node);
        }
    }

    /**
     * Run main program logic for connection
     */
    @Override
    public void run() {
        System.out.println("[TRACKER] New connection!");
        while (!this.connection.isClosed()) {
            Request request = receiveRequest();
            if (request == null) continue;

            RequestType requestType = request.getRequestType();
            if (requestType.equals(RequestType.REQUEST_PEERS)) {
                try {
                    this.serveRequestPeers(request);
                } catch (ConnectionException e) {
                    e.printStackTrace();
                }
            } else if (requestType.equals(Request.RequestType.PEER_MEMBERSHIP)) {
                this.addMembership(request);
            } else if (requestType.equals(RequestType.SEED_PIECE)) {
                this.serveRequestSeedPiece(request);
            } else if (requestType.equals(RequestType.PEER_HEARTBEAT)) {
                this.handleHeartbeat(request);
            }
        }
    }
}
